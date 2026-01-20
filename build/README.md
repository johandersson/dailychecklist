# Daily Checklist with Focus Timer

Welcome to Daily Checklist! This guide will help you get started with managing your daily tasks, custom checklists, reminders, and using the focus timer.

Stay organized and boost your productivity with the **Daily Checklist** application! This Java-based desktop program helps you manage your daily tasks efficiently while incorporating a built-in **Focus Timer** to keep you on track during focused work sessions.

## Features

- **Daily Task Management**: Add, edit, rename, and remove tasks for your morning and evening routines with flexible scheduling (daily, weekday, or specific days).
- **Custom Checklists**: Create named checklists for specific projects, habits, or one-off task lists.
- **Reminders**: Set date/time reminders for any checklist with notifications.
- **Focus Timer**: Start a customizable timer for any task to maintain focus and track progress with visual indicators.
- **Split-Pane UI**: Modern interface with daily tasks on the left and custom checklists on the right.
- **Interactive Features**: Context menus, drag-and-drop reordering, keyboard shortcuts, and visual feedback.
- **Data Persistence**: Tasks saved to XML files, reminders in properties files for fast access.
- **Cross-Platform**: Runs on Windows, macOS, and Linux (requires Java).

## Why Pure Java?

Daily Checklist is built as a **pure Java application with zero external dependencies**, offering several key advantages:

- **No Dependencies**: Runs on any system with Java installed - no additional libraries, frameworks, or package managers required
- **Small Size**: Lightweight JAR file (under 100KB) with minimal footprint
- **Fast Startup**: No dependency loading overhead
- **Secure**: Uses only built-in Java APIs, reducing potential security vulnerabilities from third-party libraries
- **Portable**: Single JAR file that works across all platforms
- **XML Storage**: Leverages Java's built-in XML processing capabilities for data persistence, ensuring compatibility and security

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
   - **Windows**: `.\build.bat`
   - **Linux/macOS**: `./build.sh`
3. The JAR will be generated in the `target/` directory.

- **Clean Build**: Delete the `target/` folder and re-run the build script.
- **Note**: The build process compiles Java sources and includes resources, documentation, and license files.

## Usage

### Starting the Application

- Double-click `dailychecklist.jar` or run `java -jar dailychecklist.jar`.
- The main window appears with a split-pane layout: daily tasks on the left, custom checklists on the right.

### Managing Daily Tasks

#### 1. Add a New Daily Task
- Click the "Add Task" button (or Ctrl+A).
- Enter the task name in the text field.
- Select the time of day (Morning or Evening).
- Choose the frequency:
  - **Daily**: Every day.
  - **Weekday**: Monday to Friday.
  - **Specific Day**: Choose from Monday to Sunday.
- Click "Add Task" to save it.

#### 2. View and Interact with Daily Tasks
- The left panel shows tasks grouped by Morning/Evening.
- Check "Show weekday tasks" to include weekday-specific tasks.
- Right-click a task for options:
  - **Start Focus Timer**: Begin a timed session.
  - **Rename**: Edit the task name.
  - **Remove**: Delete the task.
- Check/uncheck tasks to mark them complete.
- Drag and drop to reorder tasks.

### Managing Custom Checklists

#### 1. Create a Custom Checklist
- In the right panel, enter a name in the text field.
- Click "Create Checklist".
- The checklist appears in the list and is automatically selected.

#### 2. Add Tasks to Custom Checklists
- Select a checklist from the list.
- Click "Add Task" in the checklist view.
- Enter the task name and click "Add".

#### 3. Manage Checklists and Tasks
- Right-click a checklist name for options:
  - **Rename**: Change the checklist name.
  - **Delete**: Remove the checklist and its tasks.
  - **Add Reminder**: Set a date/time reminder.
  - **View/Edit Reminders**: Manage existing reminders.
- Right-click tasks within checklists to rename or remove them.

### Using Reminders

#### 1. Add Reminders
- Right-click a checklist and select "Add Reminder".
- Set the date and time.
- The app checks for reminders every minute.

#### 2. Manage Reminders
- Right-click a checklist and select "View/Edit Reminders".
- Edit or delete existing reminders.
- When reminder time arrives, choose to open the checklist or mark as done.

### Using the Focus Timer

- Right-click any task and select "Start Focus Timer."
- Set the timer duration (in minutes) and click "OK."
- The timer window appears:
  - Displays remaining time.
  - Circle panel shows progress (each circle = 5 minutes).
  - Use keyboard shortcuts (Space to pause/resume).
- When time expires, a notification alerts you.

### Settings

- Access via the menu bar.
- Configure application preferences.

### Keyboard Shortcuts
- **Add Task**: Ctrl+A
- **Refresh Tasks**: F5
- **Timer Controls**: Space (pause/resume), Enter (start)

## Configuration

- **Data Storage**: Tasks are stored in `dailychecklist-tasks.xml`, reminders in `dailychecklist-reminders.properties` in the user home directory.
- **Settings**: Managed via the Settings dialog (persisted in user preferences).
- **Performance**: In-memory caching ensures fast access to frequently used data.

## Troubleshooting

- **Build Issues**: Ensure JDK is installed. Check for compilation errors in the console.
- **Runtime Errors**: Verify Java version (`java -version`). Ensure no conflicting JARs.
- **Data Loss**: Backup XML and properties files regularly.
- **Checklists Not Appearing**: New checklists appear immediately after creation. If not visible after restart, they may not have tasks.
- **Reminders Not Working**: Ensure the app is running for notifications. Reminders are checked every minute.
- **Performance Issues**: The app uses efficient caching; restart if UI becomes unresponsive.

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
