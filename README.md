# Daily Checklist with Focus Timer

Stay organized and boost your productivity with the **Daily Checklist** application! This Java-based desktop program helps you manage your daily tasks efficiently while incorporating a built-in **Focus Timer** to keep you on track during focused work sessions.

## Features

- **Task Management**: Add, edit, rename, and remove tasks for your morning and evening routines.
- **Customizable Scheduling**: Assign tasks to specific days (e.g., Monday, Tuesday), or set them as daily or weekday tasks.
- **Focus Timer**: Start a customizable timer for any task to maintain focus and track progress with visual indicators.
- **Interactive UI**: User-friendly interface with context menus, tabbed navigation, and drag-and-drop support for reordering tasks.
- **Visual Feedback**: Dynamic circle panel to track completed time chunks (each circle represents a 5-minute interval).
- **Modern About Dialog**: View application information, version, and credits in a sleek dialog.
- **Data Persistence**: Tasks are saved to XML files for persistence across sessions.
- **Robust Error Handling**: Comprehensive error messages for file operations, data validation, and user inputs.
- **Keyboard Shortcuts**: Quick actions via key bindings for efficiency.
- **Cross-Platform**: Runs on Windows, macOS, and Linux (requires Java).

## Requirements

- **Java**: JDK 8 or higher (for compilation and runtime).
- **Operating System**: Windows, macOS, or Linux.

## Installation

1. **Clone or Download**: Get the project files from the repository.
2. **Build the JAR**:
   - Run `build.bat` (Windows) to compile and package the application.
   - This creates `target/dailychecklist.jar`.
3. **Run the Application**:
   - Execute `java -jar target/dailychecklist.jar` to start the app.

No additional dependencies are required for runtime.

## Building from Source

To build the JAR manually:

1. Ensure Java JDK is installed and `javac`/`jar` are in your PATH.
2. Run the build script:
   ```
   .\build.bat
   ```
3. The JAR will be generated in the `target/` directory.

- **Clean Build**: Delete the `target/` folder and re-run `build.bat`.
- **Note**: The build process compiles Java sources and includes resources (e.g., MANIFEST.MF).

## Usage

### Starting the Application

- Double-click `dailychecklist.jar` or run `java -jar dailychecklist.jar`.
- The main window appears with tabs for Checklist, Add Task, and Settings.

### Managing Tasks

#### 1. Add a New Task
- Navigate to the "Add Task" tab.
- Enter the task name in the text field.
- Select the time of day (Morning or Evening).
- Choose the frequency:
  - **Daily**: Every day.
  - **Weekday**: Monday to Friday.
  - **Specific Day**: Choose from Monday to Sunday.
- Click "Add Task" to save it.

#### 2. View and Interact with Tasks
- Go to the "Checklist" tab.
- Tasks are listed by time of day (Morning/Evening).
- Right-click a task for options:
  - **Start Focus Timer**: Begin a timed session.
  - **Rename**: Edit the task name.
  - **Remove**: Delete the task.
- Drag and drop to reorder tasks.

#### 3. Using the Focus Timer
- Right-click a task and select "Start Focus Timer."
- Set the timer duration (in minutes) and click "OK."
- The timer window appears:
  - Displays remaining time.
  - Circle panel shows progress (each circle = 5 minutes).
  - Use keyboard shortcuts (e.g., Space to pause/resume).
- When time expires, a notification alerts you.

#### 4. Settings
- Access via the menu bar.
- Configure application preferences (e.g., default timer durations).

### Keyboard Shortcuts
- **Add Task**: Ctrl+A (or similar, check menu).
- **Refresh Tasks**: F5.
- **Timer Controls**: Space (pause/resume), Enter (start).

## Configuration

- **Data Storage**: Tasks are stored in `tasks.xml` in the application directory.
- **Settings**: Managed via the Settings dialog (persisted in user preferences).
- **Customization**: Modify source code for advanced changes (e.g., timer intervals).

## Troubleshooting

- **Build Issues**: Ensure JDK is installed. Check for compilation errors in the console.
- **Runtime Errors**: Verify Java version (`java -version`). Ensure no conflicting JARs.
- **Data Loss**: Backup `tasks.xml` regularly.
- **Performance**: The app is lightweight; close other apps if issues occur.

## Development

- **Project Structure**:
  - `src/main/java/`: Source code.
  - `src/main/resources/`: Resources (e.g., MANIFEST.MF).
  - `src/test/java/`: Unit tests (requires JUnit for running).
  - `build.bat`: Build script.
- **Testing**: Run tests manually with JUnit (not included in build).
- **Contributing**: Fork the repo, make changes, and submit a pull request.

## License

This project is licensed under the terms specified in the [LICENSE](license.md) file. See the license for details on usage, distribution, and modifications.

## Support

- **Issues**: Report bugs or request features via the repository's issue tracker.
- **Documentation**: Refer to this README or inline code comments.
- **Contact**: Check the About dialog in the app for developer info.

Enjoy staying organized and focused with Daily Checklist!
