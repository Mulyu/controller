package com.mulyu.controller.shizuku;

// Runs inside a Shizuku UserService process (shell, or root if the user
// granted Shizuku via root) so it can spawn /system/bin/uinput, which needs
// shell/root privilege to open /dev/uinput on stock Android.
interface IUinputService {

    // Starts `uinput -` as a child process and sends the register command
    // for the given profile. Returns false if the uinput binary could not
    // be started or the process died immediately.
    boolean start(String registerCommandJson);

    // Writes one inject command (a line of JSON) to the running uinput
    // process's stdin. No-op if start() has not succeeded.
    void sendCommand(String commandJson);

    // True while the uinput child process is alive.
    boolean isAlive();

    // Terminates the uinput child process, which removes the virtual device.
    void destroy();
}
