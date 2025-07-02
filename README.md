# NeuroKeyCoder

A custom Android keyboard built with Kotlin that provides a unique typing experience.

## Features

- **Custom Keyboard Layout**: QWERTY layout with special function keys
- **Material Design**: Modern UI following Material Design principles
- **Easy Setup**: Simple activation through Android settings
- **Extensible**: Built with modular architecture for easy customization

## Project Structure

```
NeuroKeyCoder/
├── app/
│   ├── src/main/
│   │   ├── java/com/neurokeycoder/
│   │   │   ├── MainActivity.kt              # Main app activity
│   │   │   └── keyboard/
│   │   │       ├── NeuroKeyboardService.kt  # Keyboard service
│   │   │       └── view/
│   │   │           ├── NeuroKeyboardView.kt # Main keyboard view
│   │   │           └── keyboard/
│   │   │               ├── KeyboardRowView.kt # Individual row view
│   │   │               └── KeyboardRow.kt    # Row data class
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   └── activity_main.xml        # Main activity layout
│   │   │   ├── values/
│   │   │   │   ├── strings.xml              # String resources
│   │   │   │   ├── colors.xml               # Color definitions
│   │   │   │   └── themes.xml               # App themes
│   │   │   └── xml/
│   │   │       ├── method.xml               # Input method config
│   │   │       ├── backup_rules.xml         # Backup configuration
│   │   │       └── data_extraction_rules.xml # Data extraction config
│   │   └── AndroidManifest.xml              # App manifest
│   └── build.gradle                         # App module build config
├── build.gradle                             # Root build config
├── settings.gradle                          # Project settings
├── gradle.properties                        # Gradle properties
└── README.md                               # This file
```

## Setup Instructions

### Prerequisites

- Android Studio Arctic Fox or later
- Android SDK 21+ (API level 21)
- Java 8 or later

### Building the Project

1. **Clone the repository**:
   ```bash
   git clone <repository-url>
   cd NeuroKeyCoder
   ```

2. **Open in Android Studio**:
   - Launch Android Studio
   - Select "Open an existing Android Studio project"
   - Navigate to the NeuroKeyCoder directory and select it

3. **Build the project**:
   ```bash
   ./gradlew build
   ```

4. **Install on device**:
   ```bash
   ./gradlew installDebug
   ```

### Enabling the Keyboard

1. **Install the app** on your Android device
2. **Open the app** and tap "Enable Keyboard"
3. **Go to Android Settings** → System → Languages & input → Virtual keyboard
4. **Enable "NeuroKeyCoder Keyboard"**
5. **Set as default** (optional) or switch between keyboards using the keyboard icon in the input field

## Usage

### Basic Typing
- Use the QWERTY layout for standard typing
- Tap "SPACE" to add spaces
- Tap "BACKSPACE" to delete characters
- Tap "SHIFT" to toggle case (future enhancement)

### Switching Keyboards
- When typing, tap the keyboard icon in the input field
- Select "NeuroKeyCoder Keyboard" from the list

## Development

### Adding New Features

1. **Custom Keys**: Modify `NeuroKeyboardView.kt` to add new keys
2. **Layout Changes**: Update the keyboard layout in `setupKeyboard()`
3. **Styling**: Modify colors and themes in the `res/values/` directory
4. **Functionality**: Extend `NeuroKeyboardService.kt` for new input handling

### Key Components

- **NeuroKeyboardService**: Main service that handles input method functionality
- **NeuroKeyboardView**: Custom view that renders the keyboard layout
- **KeyboardRowView**: Manages individual rows of keys
- **MainActivity**: Provides UI for enabling and configuring the keyboard

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Troubleshooting

### Common Issues

1. **Keyboard not appearing**: Make sure the keyboard is enabled in Android settings
2. **Build errors**: Ensure you have the correct Android SDK version installed
3. **App crashes**: Check logcat for detailed error messages

### Debug Mode

To enable debug logging, add this to your `build.gradle`:
```gradle
buildTypes {
    debug {
        debuggable true
    }
}
```

## Future Enhancements

- [ ] Shift key functionality
- [ ] Number row
- [ ] Symbol keyboard
- [ ] Custom themes
- [ ] Auto-complete suggestions
- [ ] Swipe typing
- [ ] Multiple language support 