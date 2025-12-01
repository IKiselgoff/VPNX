
import Cocoa
import Darwin

class AppDelegate: NSObject, NSApplicationDelegate {
    private var statusItem: NSStatusItem!
    private let vpnxPath = NSString(string: "~/.vpnx/bin/vpnx").expandingTildeInPath
    private let configPath = NSString(string: "~/.vpnx/config.json").expandingTildeInPath
    private let logPath    = NSString(string: "~/.vpnx/xray.log").expandingTildeInPath
    private let pidPath    = NSString(string: "~/.vpnx/xray.pid").expandingTildeInPath

    func applicationDidFinishLaunching(_ notification: Notification) {
        statusItem = NSStatusBar.system.statusItem(withLength: NSStatusItem.variableLength)
        statusItem.button?.title = "VPNX • OFF"
        rebuildMenu()
        Timer.scheduledTimer(withTimeInterval: 3, repeats: true) { _ in
            self.updateTitleWithStatus()
        }
    }

    // MARK: - Menu

    private func rebuildMenu() {
        let menu = NSMenu()

        // Status line from vpnx (for details), but ON/OFF is computed by PID existence.
        let statusText = runVPNX(["status"]).output.trimmingCharacters(in: .whitespacesAndNewlines)
        menu.addItem(withTitle: "Status: " + statusText, action: nil, keyEquivalent: "")

        menu.addItem(NSMenuItem(title: "Start (default)", action: #selector(startDefault), keyEquivalent: "s"))
        menu.addItem(NSMenuItem(title: "Stop", action: #selector(stop), keyEquivalent: "x"))

        let switchItem = NSMenuItem(title: "Switch…", action: nil, keyEquivalent: "")
        let switchMenu = NSMenu()
        for tag in listTags() {
            let item = NSMenuItem(title: tag, action: #selector(switchToTag(_:)), keyEquivalent: "")
            item.representedObject = tag
            switchMenu.addItem(item)
        }
        switchMenu.addItem(NSMenuItem.separator())
        for preset in ["NL","DE","LV","US","IN","RU"] {
            let item = NSMenuItem(title: "Preset: \(preset)", action: #selector(switchToPreset(_:)), keyEquivalent: "")
            item.representedObject = preset
            switchMenu.addItem(item)
        }
        switchItem.submenu = switchMenu
        menu.addItem(switchItem)

        menu.addItem(NSMenuItem(title: "Set Default…", action: #selector(setDefaultPrompt), keyEquivalent: "d"))
        menu.addItem(NSMenuItem(title: "Import VLESS…", action: #selector(importVLESS), keyEquivalent: "i"))
        menu.addItem(NSMenuItem(title: "Restart", action: #selector(restart), keyEquivalent: "r"))

        menu.addItem(NSMenuItem.separator())
        menu.addItem(NSMenuItem(title: "Open Logs", action: #selector(openLogs), keyEquivalent: "l"))
        menu.addItem(NSMenuItem(title: "Open Config", action: #selector(openConfig), keyEquivalent: "o"))

        menu.addItem(NSMenuItem.separator())
        menu.addItem(NSMenuItem(title: "Quit", action: #selector(quit), keyEquivalent: "q"))

        statusItem.menu = menu
        updateTitleWithStatus()
    }

    // MARK: - Actions

    @objc private func startDefault() {
        _ = runVPNX(["start"])
        updateTitleWithStatus()
        rebuildMenu()
    }

    @objc private func stop() {
        _ = runVPNX(["stop"])
        updateTitleWithStatus()
        rebuildMenu()
    }

    @objc private func restart() {
        _ = runVPNX(["stop"])
        _ = runVPNX(["start"])
        updateTitleWithStatus()
        rebuildMenu()
    }

    @objc private func switchToTag(_ sender: NSMenuItem) {
        guard let tag = sender.representedObject as? String else { return }
        _ = runVPNX(["switch", tag])
        updateTitleWithStatus()
        rebuildMenu()
    }

    @objc private func switchToPreset(_ sender: NSMenuItem) {
        guard let cc = sender.representedObject as? String else { return }
        _ = runVPNX(["switch", cc])
        updateTitleWithStatus()
        rebuildMenu()
    }

    @objc private func setDefaultPrompt() {
        let alert = NSAlert()
        alert.messageText = "Set Default Outbound"
        alert.informativeText = "Enter TAG (use `vpnx list` in Terminal to see all tags)."
        let input = NSTextField(frame: NSRect(x: 0, y: 0, width: 260, height: 24))
        alert.accessoryView = input
        alert.addButton(withTitle: "Set")
        alert.addButton(withTitle: "Cancel")
        if alert.runModal() == .alertFirstButtonReturn {
            let tag = input.stringValue.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !tag.isEmpty else { return }
            _ = runVPNX(["set-default", tag])
            updateTitleWithStatus()
            rebuildMenu()
        }
    }

    @objc private func importVLESS() {
        let alert = NSAlert()
        alert.messageText = "Import VLESS"
        alert.informativeText = "Enter TAG and VLESS URL (type=xhttp)."
        let stack = NSStackView()
        stack.orientation = .vertical
        stack.spacing = 8
        let tagField = NSTextField(string: "")
        tagField.placeholderString = "TAG (e.g., NL-2025)"
        let urlField = NSTextField(string: "")
        urlField.placeholderString = "vless://...type=xhttp..."
        let check = NSButton(checkboxWithTitle: "Set as default", target: nil, action: nil)
        stack.addArrangedSubview(tagField)
        stack.addArrangedSubview(urlField)
        stack.addArrangedSubview(check)
        stack.setFrameSize(NSSize(width: 360, height: 88))

        let dlg = NSAlert()
        dlg.messageText = "Import VLESS"
        dlg.accessoryView = stack
        dlg.addButton(withTitle: "Import")
        dlg.addButton(withTitle: "Cancel")
        if dlg.runModal() == .alertFirstButtonReturn {
            let tag = tagField.stringValue.trimmingCharacters(in: .whitespacesAndNewlines)
            let url = urlField.stringValue.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !tag.isEmpty, url.lowercased().hasPrefix("vless://") else { return }
            var args = ["import", tag, url]
            if check.state == .on { args.append("--default") }
            _ = runVPNX(args)
            updateTitleWithStatus()
            rebuildMenu()
        }
    }

    @objc private func openLogs() {
        NSWorkspace.shared.open(URL(fileURLWithPath: logPath))
    }

    @objc private func openConfig() {
        NSWorkspace.shared.open(URL(fileURLWithPath: configPath))
    }

    @objc private func quit() {
        NSApplication.shared.terminate(nil)
    }

    // MARK: - Status detection via PID

    private func isRunningByPID() -> Bool {
        let fm = FileManager.default
        guard fm.fileExists(atPath: pidPath),
              let pidStr = try? String(contentsOfFile: pidPath, encoding: .utf8),
              let pid = Int32(pidStr.trimmingCharacters(in: .whitespacesAndNewlines)) else {
            return false
        }
        // kill(pid, 0) == 0 if process exists and we can send a signal
        return kill(pid, 0) == 0
    }

    private func updateTitleWithStatus() {
        DispatchQueue.main.async {
            self.statusItem.button?.title = self.isRunningByPID() ? "VPNX • ON" : "VPNX • OFF"
        }
    }

    // MARK: - Helpers

    private func listTags() -> [String] {
        let out = runVPNX(["list"]).output
        return out.split(separator: "\n").map { String($0).trimmingCharacters(in: .whitespacesAndNewlines) }.filter { !$0.isEmpty }
    }

    @discardableResult
    private func runVPNX(_ args: [String]) -> (output: String, exitCode: Int32) {
        let task = Process()
        task.executableURL = URL(fileURLWithPath: "/usr/bin/env")
        task.arguments = ["bash", NSString(string: vpnxPath).expandingTildeInPath] + args
        let pipe = Pipe()
        task.standardOutput = pipe
        task.standardError = pipe
        do {
            try task.run()
        } catch {
            return ("Error: \(error.localizedDescription)", -1)
        }
        task.waitUntilExit()
        let data = pipe.fileHandleForReading.readDataToEndOfFile()
        let str = String(data: data, encoding: .utf8) ?? ""
        return (str, task.terminationStatus)
    }
}
