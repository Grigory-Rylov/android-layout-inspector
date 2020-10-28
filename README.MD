# Yet another android layout inspector

More stable Android Layout inspector than Android Studio Layout Inspector.
Allows you to switch between displaying dimensions in **PX** and **DP** (only for new layout captures, not for opened files)

![preview](assets/preview.png)

![Searching mode](assets/find.png)

## Dark theme

![Themes](assets/themes.png)

## Download
![GitHub All Releases](https://img.shields.io/github/downloads/Grigory-Rylov/android-layout-inspector/total?color=%234caf50&style=for-the-badge)  
- [Download latest release](https://github.com/Grigory-Rylov/android-layout-inspector/releases)

## Launching
1) Ensure you've set `ANDROID_HOME` environment variable

2) Call `java -jar android-layout-inspector-{VERSION}.jar`

Or just double click on jar and enter path to Android SDK manually (it will be stored into settings file)

## Why Yet another android layout inspector?
Because AS layout inspector sometimes cannot download layouts for some reasons.
Also you can switch to *DP* dimension mode.

## Measure distance between two element
1) Select first element by **Mouse click** - it will be select by red square
2) Select second element by **Mouse click + Ctrl(Cmd)** - it will be select by yellow square.

In the status bar will shown distance between first and second
![measurement](assets/measurements.png)

## Measure distance with ruler
1) Press **Shift** and move mouse to see where will be first point of ruler (black rectangle)
2) **Mouse click + Shift** to start ruler mode
3) **Mouse drag + Shift** to change ruler size
You will see ruler size in current units in status bar
 
## Hotkeys
### Files
**Ctrl + o** - Open file dialog

**Ctrl + n** - Record new layout

### Layout tree
**Ctrl + c** - Copy node name

**Ctrl + Shift + c** - Copy node ID

**Mouse click + Ctrl** - Select element to measure distance from selected to current.

**Mouse drag + Shift** - Measure ruler.

**Mouse right click** - Show distance from selected view to current point.
### Properties table
**Ctrl + c** - Copy property value

### Find
**Ctrl + f** - Open find dialog (type text and press **Enter**)

### Layouts
**z** - reset zoom to 100%
**f** - fit zoom to layout panel width

## Troubleshooting
- If you cannot see your process in processes window - try to press **Reset ADB** button.

- Sometimes Android Studio holds debug ports and Layout Inspector cannot find applications on device.
Try to close Android Studio, press **Reset ADB** button.

## Settings
Settings are stored in `android-layout-inspector-settings.json` :

 - `shouldStopAdbAfterJob` allows to forcibly close ADB after operations and after exit
 - `remoteDeviceAddress` if not empty - try to connect to remote device with address `"remoteDeviceAddress"` and before connection to ADB (experimental)

<a href="https://www.buymeacoffee.com/grishberg" target="_blank"><img src="https://cdn.buymeacoffee.com/buttons/v2/default-blue.png" alt="Buy Me A Coffee" style="height: 60px !important;width: 217px !important;" ></a>

# License

Yet another android layout inspector is released under the [Apache License, Version 2.0](LICENSE.txt).
