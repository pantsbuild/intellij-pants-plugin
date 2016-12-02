# intellij-pants-plugin

* The intellij-pants-plugin supports importing, compiling and testing [Pants](http://pantsbuild.github.io/) projects.
* Scala and Java projects are fully supported. Python projects are supported on a best effort basis.
* As of 12/2/2016, latest version of the plugin supports IntelliJ IDEA 2016.3 and up for both Community Edition and Ultimate Edition.
* As of 12/2/2016, the plugin supports [1.1.x](https://pantsbuild.github.io/notes-1.1.x.html), [1.2.x](https://pantsbuild.github.io/notes-1.2.x.html), and current master of Pants.

## User documentation

### Installing the Plugin
Please use `Plugins` tab: (Main menu: Settings | Plugins) to install the plugin.
Find `Pants Support` plugin. Install and Restart IntelliJ.

### Minimum set of plugins is required to support `Pants Support` features
  * Gradle
  * Groovy
  * Java Bytecode Decompiler
  * JUnit
  * Pants Support
  * Python Community Edition (if you are importing python projects)
  * Scala

#### Importing an entire project directory
  * Use Main menu: File -> New -> Project From Existing Sources
  * Select project directory
     ![Import project from directory](images/import_dir1.png)
  * Choose "Pants" on the next screen
  * Make sure the check box "All Targets in the directory" is enabled and proceed with the wizard

#### Importing targets from a BUILD File
  * Use Main menu: File -> New -> Project From Existing Sources
  * Select a Build File from within the project
     ![Import project from BUILD file](images/import_file1.png)
  * Check the targets you want to Import and proceed with the wizard. (Please wait for the targets to show up)
     ![Choose Targets](images/import_file2.png)

#### Importing targets from a script
  * Use Main menu: File -> New -> Project From Existing Sources
  * Select an executable that will use export goal to produce a desirable project structure.
    See [an integration test](testData/testprojects/intellij-integration/export1.sh) as an example.

#### Importing multiple BUILD files/directories
  * Import the first directory/BUILD file
  * Use File -> New -> Module From Existing Sources to import next directories/BUILD files

Once you import the project using above steps, you will see the "Project View" with multiple modules configured.

### Invoking Pants within IntelliJ
The plugin can invoke any Pants commands via Pants Tasks.
  * To configure a Pants Task simply create a new Pants Run Configuration
    ![Creating of a new Pants Run Configuration](images/tasks/add_pants_run_config.png)
  * Choose a target to run a task for
    ![Choosing a target](images/tasks/configure_target.png)
    ![Configured target](images/tasks/configured_target.png)
  * Fill the rest of options for the task. Note there is a task for each Pants goal.
    ![Configure Pants Task](images/tasks/configure_pants_task.png)
  * Run the Task
    ![Configured Pants Task](images/tasks/task_run.png)
  * To debug a task simply press Debug button next to Run button.
  * Note: you can create a task for any goal
    ![Bundle Task](images/tasks/bundle_task.png)

#### Predefined Pants Tasks for test targets
The plugin can also generate test configurations.

For example if a test class is opened then with a right click it's easy to create a task to run and debug
    ![Run Configuration Producer](images/tasks/create_task_from_context_single.png)
    ![Preconfigured Run Configuration](images/tasks/preconfigured_task_single_test.png)

With a right click in Project View it's easy to create a test task to run all tests for a target
    ![Run Configuration Producer](images/tasks/create_task_from_context_all.png)

### Compilation
* Pants' compile goal (Default)
  The plugin will use `pants compile <list of targets>` to compile your project once a `Make` or `PantsCompile` command is invoked.
  ![Compilation via compile goal ](images/compilation_via_compile_goal.png)

Compilation options can be configured in Preferences -> Build, Execution, Deployment -> Compiler -> Pants:
![Compilation Options](images/compilation_options.png)

### Plugin Features.
* Project File Tree View.
  The plugin configures modules per pants build target. Due to multiple modules, the default "Project View" is not very user friendly.
  The Plugin provides a custom view "Project Files Tree View". This view adheres to your repository file hierarchy.
  You can switch to this view as follows:
  ![Project Files Tree View](images/project_files_tree_view.png)
  "Project Files Tree View" also provides an ability to filter out files that weren't loaded during project generation.
  ![Show Only Loaded Files](images/show_only_loaded_files.jpg)
* Project Regeneration using IntelliJ Action.
  If you add a dependency to your project, you can re-resolve project using IntelliJ Action in background.
  Use Main Menu: Help -> Find Action or Short hand Cmd+Shift+A and select Action "Refresh all External Projects"
  Remember to check "Include non-menu actions"
  ![Refresh Project](images/refresh_action.png)
* Running tests within IntelliJ
  You can right click on tests and run tests.

### Report Bugs
If you encounter a bug, please check for existing issues or file a new one on [the project page](https://github.com/pantsbuild/intellij-pants-plugin/issues).
