package com.ikiselgoff.vpnx;

interface IShellService {
    String execute(String command);
    int uid();
    void destroy();
}
