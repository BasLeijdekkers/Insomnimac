# Insomniac

An IntelliJ IDEA plugin that prevents your system from sleeping while a 
long-running task is active in your IntelliJ Platform IDE.

Too often I have started a full rebuild or an inspection run, went for coffee, and twenty minutes later when
I came back: my laptop has gone into sleep mode and the task is only half finished. To prevent this from
happening, I built this plugin. It detects tasks that are active for more than one-and-a-half minutes, and when
it has found one, keeps the system awake. Zero user interface, just set it and forget it.

On the Mac [NSProcessInfo][1] is used to prevent sleep.
To prevent sleep, on the Mac the built-in-the-OS tool caffeinate is used. On Windows the OS
[SetThreadExecutionState function][2] is called. And on Linux the mouse is 
wiggled by one pixel
(if you know of a better way, let me know or send a pull request).

[1]: https://developer.apple.com/documentation/foundation/nsprocessinfo/1415995-beginactivitywithoptions
[2]: https://docs.microsoft.com/en-us/windows/win32/api/winbase/nf-winbase-setthreadexecutionstate