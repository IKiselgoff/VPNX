import Cocoa
import Darwin

final class AppDelegate: NSObject, NSApplicationDelegate, NSTableViewDataSource, NSTableViewDelegate, NSSearchFieldDelegate {
    private let root = FileManager.default.homeDirectoryForCurrentUser.appendingPathComponent(".vpnx")
    private var item: NSStatusItem!
    private var window: NSWindow!
    private let headline = NSTextField(labelWithString: "VPNX")
    private let details = NSTextField(wrappingLabelWithString: "")
    private let search = NSSearchField()
    private let subscriptions = NSPopUpButton()
    private let table = NSTableView()
    private let feedback = NSTextField(wrappingLabelWithString: "")
    private var data: [String: Any] = [:]
    private var rows: [[String: Any]] = []
    private var busy = false
    private var actions: [NSButton] = []

    func applicationDidFinishLaunching(_ notification: Notification) {
        item = NSStatusBar.system.statusItem(withLength: NSStatusItem.variableLength)
        let menu = NSMenu()
        for (title, selector) in [("Открыть VPNX", #selector(showWindow)), ("Обновить подписки", #selector(sync)), ("Подключить", #selector(connect)), ("Отключить", #selector(disconnect))] {
            let entry = NSMenuItem(title: title, action: selector, keyEquivalent: "")
            entry.target = self
            menu.addItem(entry)
        }
        menu.addItem(.separator())
        let quit = NSMenuItem(title: "Закрыть окно и меню", action: #selector(quitApp), keyEquivalent: "")
        quit.target = self
        menu.addItem(quit)
        item.menu = menu
        createWindow()
        refresh()
        Timer.scheduledTimer(withTimeInterval: 10, repeats: true) { [weak self] _ in self?.refresh() }
        if CommandLine.arguments.contains("--show") || CommandLine.arguments.contains("--snapshot") { showWindow() }
    }

    func applicationShouldHandleReopen(_ sender: NSApplication, hasVisibleWindows flag: Bool) -> Bool {
        showWindow()
        return true
    }

    private func button(_ title: String, _ action: Selector) -> NSButton {
        let b = NSButton(title: title, target: self, action: action)
        b.bezelStyle = .rounded
        actions.append(b)
        return b
    }

    private func createWindow() {
        window = NSWindow(contentRect: NSRect(x: 0, y: 0, width: 900, height: 640),
                          styleMask: [.titled, .closable, .miniaturizable, .resizable], backing: .buffered, defer: false)
        window.title = "VPNX"
        window.minSize = NSSize(width: 760, height: 520)
        window.isReleasedWhenClosed = false
        window.center()
        let content = window.contentView!
        let stack = NSStackView()
        stack.orientation = .vertical
        stack.alignment = .leading
        stack.spacing = 14
        stack.translatesAutoresizingMaskIntoConstraints = false
        content.addSubview(stack)
        NSLayoutConstraint.activate([
            stack.leadingAnchor.constraint(equalTo: content.leadingAnchor, constant: 24),
            stack.trailingAnchor.constraint(equalTo: content.trailingAnchor, constant: -24),
            stack.topAnchor.constraint(equalTo: content.topAnchor, constant: 24),
            stack.bottomAnchor.constraint(equalTo: content.bottomAnchor, constant: -24)
        ])
        headline.font = NSFont(name: "AvenirNext-DemiBold", size: 28) ?? .boldSystemFont(ofSize: 28)
        stack.addArrangedSubview(headline)
        details.textColor = .secondaryLabelColor
        stack.addArrangedSubview(details)
        let controls = NSStackView(views: [button("Подключить", #selector(connect)), button("Отключить", #selector(disconnect)),
                                         button("Перезапустить", #selector(restart)), button("Проверить интернет", #selector(diagnose))])
        controls.spacing = 8
        stack.addArrangedSubview(controls)
        let filters = NSStackView()
        filters.spacing = 12
        search.placeholderString = "Поиск страны, профиля или протокола"
        search.delegate = self
        search.widthAnchor.constraint(greaterThanOrEqualToConstant: 330).isActive = true
        subscriptions.target = self
        subscriptions.action = #selector(filterChanged)
        filters.addArrangedSubview(search)
        filters.addArrangedSubview(subscriptions)
        stack.addArrangedSubview(filters)
        for (identifier, title, width) in [("name", "Профиль", 400.0), ("protocol", "Протокол", 120.0), ("transport", "Транспорт", 120.0), ("active", "Состояние", 130.0)] {
            let column = NSTableColumn(identifier: NSUserInterfaceItemIdentifier(identifier))
            column.title = title
            column.width = width
            table.addTableColumn(column)
        }
        table.delegate = self
        table.dataSource = self
        table.rowHeight = 34
        table.usesAlternatingRowBackgroundColors = true
        table.doubleAction = #selector(connect)
        table.target = self
        let scroll = NSScrollView()
        scroll.hasVerticalScroller = true
        scroll.documentView = table
        stack.addArrangedSubview(scroll)
        scroll.widthAnchor.constraint(equalTo: stack.widthAnchor).isActive = true
        scroll.heightAnchor.constraint(greaterThanOrEqualToConstant: 210).isActive = true
        let subscriptionControls = NSStackView(views: [button("Обновить", #selector(sync)), button("Добавить подписку", #selector(addSubscription)),
                                                      button("Автообновление вкл/выкл", #selector(toggleSubscription)), button("Удалить подписку", #selector(removeSubscription))])
        subscriptionControls.spacing = 8
        stack.addArrangedSubview(subscriptionControls)
        feedback.textColor = .secondaryLabelColor
        stack.addArrangedSubview(feedback)
        feedback.widthAnchor.constraint(equalTo: stack.widthAnchor).isActive = true
        let footer = NSStackView(views: [button("Логи", #selector(openLogs)), button("Настройки подключения", #selector(openConfig))])
        stack.addArrangedSubview(footer)
    }

    @objc private func showWindow() {
        window.makeKeyAndOrderFront(nil)
        NSApp.activate(ignoringOtherApps: true)
        refresh()
    }

    private func process(_ executable: String, _ arguments: [String]) -> (String, Int32) {
        let task = Process()
        task.executableURL = URL(fileURLWithPath: executable)
        task.arguments = arguments
        let pipe = Pipe()
        task.standardOutput = pipe
        task.standardError = pipe
        do {
            try task.run()
            let output = pipe.fileHandleForReading.readDataToEndOfFile()
            task.waitUntilExit()
            return (String(data: output, encoding: .utf8) ?? "", task.terminationStatus)
        } catch { return (error.localizedDescription, -1) }
    }

    private func manager(_ args: [String]) -> (String, Int32) {
        process("/usr/bin/python3", [root.appendingPathComponent("bin/vpnx-subscriptions").path] + args)
    }

    private func perform(_ operation: @escaping () -> (String, Int32)) {
        guard !busy else { return }
        busy = true
        actions.forEach { $0.isEnabled = false }
        feedback.stringValue = "Выполняется…"
        DispatchQueue.global(qos: .userInitiated).async {
            let result = operation()
            DispatchQueue.main.async {
                self.busy = false
                self.actions.forEach { $0.isEnabled = true }
                self.feedback.stringValue = result.0.trimmingCharacters(in: .whitespacesAndNewlines)
                self.feedback.textColor = result.1 == 0 ? .secondaryLabelColor : .systemRed
                self.refresh()
            }
        }
    }

    private func refresh() {
        guard !busy else { return }
        busy = true
        DispatchQueue.global(qos: .utility).async {
            let result = self.manager(["status"])
            let parsed = result.0.data(using: .utf8).flatMap { try? JSONSerialization.jsonObject(with: $0) as? [String: Any] }
            DispatchQueue.main.async {
                self.busy = false
                guard let parsed = parsed else {
                    self.feedback.stringValue = result.0
                    return
                }
                self.data = parsed
                let running = parsed["running"] as? Bool ?? false
                self.item.button?.title = running ? "VPNX · ON" : "VPNX · OFF"
                self.headline.stringValue = running ? "VPNX  /  Подключено" : "VPNX  /  Отключено"
                let selectedID = self.subscriptions.selectedItem?.representedObject as? String ?? ""
                self.subscriptions.removeAllItems()
                self.subscriptions.addItem(withTitle: "Все подписки")
                self.subscriptions.lastItem?.representedObject = ""
                for sub in parsed["subscriptions"] as? [[String: Any]] ?? [] {
                    self.subscriptions.addItem(withTitle: sub["name"] as? String ?? "")
                    self.subscriptions.lastItem?.representedObject = sub["id"]
                }
                self.subscriptions.addItem(withTitle: "Ручные профили")
                self.subscriptions.lastItem?.representedObject = "manual"
                if let index = self.subscriptions.itemArray.firstIndex(where: { $0.representedObject as? String == selectedID }) {
                    self.subscriptions.selectItem(at: index)
                }
                self.filterChanged()
                if CommandLine.arguments.contains("--snapshot") {
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                        guard let view = self.window.contentView else { return }
                        view.layoutSubtreeIfNeeded()
                        guard let bitmap = view.bitmapImageRepForCachingDisplay(in: view.bounds) else { return }
                        view.cacheDisplay(in: view.bounds, to: bitmap)
                        if let png = bitmap.representation(using: .png, properties: [:]) {
                            try? png.write(to: self.root.appendingPathComponent("ui-preview.png"), options: .atomic)
                        }
                    }
                }
            }
        }
    }

    private func date(_ value: Any?) -> String {
        guard let seconds = value as? Double else { return "ещё не обновлялась" }
        return DateFormatter.localizedString(from: Date(timeIntervalSince1970: seconds), dateStyle: .short, timeStyle: .short)
    }

    @objc private func filterChanged() {
        let sid = subscriptions.selectedItem?.representedObject as? String ?? ""
        let query = search.stringValue.lowercased()
        let selectedTag = selectedProfile()
        rows = (data["profiles"] as? [[String: Any]] ?? []).filter { profile in
            (sid.isEmpty || profile["subscription"] as? String == sid) &&
            (query.isEmpty || ["name", "transport", "protocol"].contains { key in (profile[key] as? String ?? "").lowercased().contains(query) })
        }
        table.reloadData()
        let current = data["current"] as? String ?? ""
        if let index = rows.firstIndex(where: { $0["tag"] as? String == (selectedTag.isEmpty ? current : selectedTag) }) {
            table.selectRowIndexes(IndexSet(integer: index), byExtendingSelection: false)
        }
        let subs = data["subscriptions"] as? [[String: Any]] ?? []
        if let sub = subs.first(where: { $0["id"] as? String == (sid.isEmpty ? "bird" : sid) }) {
            let auto = sub["enabled"] as? Bool ?? true
            let error = sub["error"] as? String ?? ""
            details.stringValue = "\(rows.count) профилей · Автообновление: \(auto ? "вкл" : "выкл") · Получено: \(date(sub["last_success"]))\nСнимок источника: \(date(sub["snapshot_updated_at"]))\(error.isEmpty ? "" : " · Ошибка: " + error)"
            if let timestamp = sub["snapshot_updated_at"] as? Double, Date().timeIntervalSince1970 - timestamp > 86400 {
                details.stringValue += " · Снимок старше суток"
            }
        } else { details.stringValue = "\(rows.count) профилей" }
    }

    func controlTextDidChange(_ obj: Notification) { filterChanged() }
    func numberOfRows(in tableView: NSTableView) -> Int { rows.count }
    func tableView(_ tableView: NSTableView, viewFor tableColumn: NSTableColumn?, row: Int) -> NSView? {
        guard row < rows.count, let key = tableColumn?.identifier.rawValue else { return nil }
        let value = key == "active" ? (rows[row]["tag"] as? String == data["current"] as? String ? "Выбран" : "") : rows[row][key] as? String ?? ""
        let label = NSTextField(labelWithString: value)
        label.lineBreakMode = .byTruncatingTail
        return label
    }
    private func selectedProfile() -> String {
        let row = table.selectedRow
        return row >= 0 && row < rows.count ? rows[row]["tag"] as? String ?? "" : ""
    }
    @objc private func connect() {
        let tag = selectedProfile().isEmpty ? data["current"] as? String ?? "" : selectedProfile()
        guard !tag.isEmpty else { feedback.stringValue = "Выберите профиль"; return }
        perform { self.process("/bin/bash", [self.root.appendingPathComponent("bin/vpnx").path, "switch", tag]) }
    }
    @objc private func disconnect() {
        perform { self.process("/bin/bash", [self.root.appendingPathComponent("bin/vpnx").path, "stop"]) }
    }
    @objc private func restart() {
        perform {
            let stopped = self.process("/bin/bash", [self.root.appendingPathComponent("bin/vpnx").path, "stop"])
            guard stopped.1 == 0 else { return stopped }
            return self.process("/bin/bash", [self.root.appendingPathComponent("bin/vpnx").path, "start"])
        }
    }
    @objc private func sync() { perform { self.manager(["sync"]) } }
    @objc private func toggleSubscription() {
        let sid = subscriptions.selectedItem?.representedObject as? String ?? ""
        guard !sid.isEmpty && sid != "manual" else { feedback.stringValue = "Выберите подписку в списке"; return }
        perform { self.manager(["toggle", sid]) }
    }
    @objc private func removeSubscription() {
        let sid = subscriptions.selectedItem?.representedObject as? String ?? ""
        guard !sid.isEmpty && sid != "manual" else { return }
        let dialog = NSAlert()
        dialog.messageText = "Удалить выбранную подписку и её профили?"
        dialog.addButton(withTitle: "Удалить")
        dialog.addButton(withTitle: "Отмена")
        guard dialog.runModal() == .alertFirstButtonReturn else { return }
        perform { self.manager(["remove", sid]) }
    }
    @objc private func addSubscription() {
        let name = NSTextField(string: "")
        name.placeholderString = "Название"
        let url = NSTextField(string: "")
        url.placeholderString = "https://… (полный Xray JSON)"
        let fields = NSStackView(views: [name, url])
        fields.orientation = .vertical
        fields.setFrameSize(NSSize(width: 420, height: 64))
        let dialog = NSAlert()
        dialog.messageText = "Добавить подписку"
        dialog.informativeText = "BIRD приходит через зеркало VPS. Дополнительные подписки: HTTPS со списком полных Xray-конфигураций."
        dialog.accessoryView = fields
        dialog.addButton(withTitle: "Добавить")
        dialog.addButton(withTitle: "Отмена")
        guard dialog.runModal() == .alertFirstButtonReturn else { return }
        let title = name.stringValue
        let address = url.stringValue
        perform {
            let added = self.manager(["add", title, address])
            guard added.1 == 0 else { return added }
            return self.manager(["sync"])
        }
    }
    @objc private func diagnose() {
        perform {
            var lines: [String] = []
            var code: Int32 = 0
            for (name, url) in [("YouTube", "https://www.youtube.com/generate_204"), ("Telegram", "https://api.telegram.org")] {
                let result = self.process("/usr/bin/curl", ["--socks5-hostname", "127.0.0.1:10808", "--max-time", "15", "-so", "/dev/null", "-w", "%{http_code}", url])
                let http = result.0.trimmingCharacters(in: .whitespacesAndNewlines)
                let ok = result.1 == 0 && ["200", "204", "301", "302"].contains(http)
                lines.append(name + ": " + (ok ? "доступен (" + http + ")" : "ошибка соединения"))
                if !ok { code = 1 }
            }
            return (lines.joined(separator: " · "), code)
        }
    }
    @objc private func openLogs() { NSWorkspace.shared.open(root.appendingPathComponent("xray.log")) }
    @objc private func openConfig() { NSWorkspace.shared.open(root.appendingPathComponent("config.json")) }
    @objc private func quitApp() { NSApp.terminate(nil) }
}
