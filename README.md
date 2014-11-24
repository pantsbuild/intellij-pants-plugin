# intellij-pants-plugin


The intelliJ-pants-plugin supports importing, compiling and testing [pants](http://pantsbuild.github.io/) projects.
Current 1.0.* version of plugin supports Scala and Java projects.

## User documentation

### Installing the Plugin
Please use “Plugins” tab: (Main menu: Settings | Plugins) to install the plugin.
Find "Pants Support" plugin. Install and Restart IntelliJ.

### Importing a Pants Project.
Using this plugin you can import entire project or specific targets in a BUILD file.

* Importing an entire project directory
    1. Use Main menu: File -> Import Project
    2. Select project directory
       ![Import project from directory](images/import_dir1.png)
    3. Choose "Pants" on the next screen
    4. Make sure the check box "All Targets in the directory" is enabled and proceed with the wizard

* Importing targets from Build File
    1. Use Main menu: File -> Import Project
    2. Select a Build File from within the project
       ![Import project from BUILD file](images/import_file1.png)
    3. Check the targets you want to Import and proceed with the wizard. (Please wait for the targets to show up)
       ![Choose Targets](images/import_file2.png)

Once you import the project using above steps, you will see the "Project View" with multiple modules configured.

Each module represents BUILD target. You can now start navigating through the project files and editing files.

The plugin will resolve the project dependencies in background using `pants goal resolve` and configure all the modules' dependencies.

Once the resolve is done, you can compile the entire project using Main menu: Build -> Compile.

You can also compile individual modules by Right clicking on the module in "Project View" and Choose "Make Module" option.

### Plugin Features.
* Project File Tree View.
  The plugin configures modules per pants build target. Due to multiple modules, the default "Project View" is not very user friendly.
  The Plugin provides a custom view "Project Files Tree View". This view adheres to your repository file hierarchy.
  You can switch to this view as follows:
  ![Project Files Tree View](images/project_files_tree_view.png)
* Build File Auto completion
  The plugin provides auto completion for Build file targets. Cntrl+Space within BUILD file will list all pants targets.
* Project Regeneration using IntelliJ Action.
  If you add a dependency to your project, you can re-resolve project using IntelliJ Action in background.
  Use Main Menu: Help -> Find Action or Short hand Cmd+Shift+A and select Action "Refresh all External Projects"
  Remember to check "Include non-menu actions"
  ![Refresh Project](images/refresh_action.png)
* Compiling within IntelliJ
* Running tests within IntelliJ
  You can right click on tests and run tests.


### Whats in near Future?
* Right click on `target` definition within BUILD file will navigate to pants target definition.
* Multiple project imports in the same window.
  <br />Right now, importing/adding another target or project to an imported project is not supported.
* Automatic regeneration of project on changes in files used for code generation.
  <br />Pants provides code generation for thrift, antrl and protobuf.
  Changing a thrift file or protobug file in source will run pants resolve in background.

### Report Bugs
If you see any bugs please file a github issue on the project page.
Attach your `idea.log` ([location instructions](https://intellij-support.jetbrains.com/entries/23352446-Locating-IDE-log-files))

For contributing to the project, continue reading below.


## Contributing Guidelines:

* Checkout the code

        git clone https://github.com/pantsbuild/intellij-pants-plugin

* Create a new branch off master to make your changes

        git checkout -b $FEATURE_BRANCH

* Run tests to verify your installation
* Post your first review ([setup instructions](http://pantsbuild.github.io/howto_contribute.html#code-review))

        ./rbt post -o -g

* Iterating over the review

        ./rbt post -o -r <RB_ID>

* Committing your change to master

        git checkout master
        git pull
        ./rbt patch -c <RB_ID>

### IntelliJ project setup:

* Download and open IntelliJ IDEA 14 Community Edition
* Install Python, Scala and Gradle Plugins (If you postpone restart, you can install them all in one go)
* Use IntelliJ IDEA 14 Community Edition as IDEA IC SDK(Project Structure(Cmd + ;) -> SDK -> '+' button -> IntelliJ Platform Plugin SDK)
* Setup the SDK's classpath
  * Add the following to the SDK's classpath
    * `~/Library/Application Support/IdeaIC14/python/lib/python-community.jar`
    * `~/Library/Application Support/IdeaIC14/Scala/lib/scala-plugin.jar`
    * `~/Library/Application Support/IdeaIC14/Scala/lib/scala-library-2.11.2.jar`
* Make sure that your project is set to configure bytecode compatible with 1.6  Preferences -> Compiler -> Java Compiler -> Project bytecode version
* Run tests to verify your installation

### Debugging the Plugin from local pants development:

* If you want to debug plugin using your local development pants, you can do so by using the property `pants.executable.path`.
  Add this configuration to Pants Run config.
  e.g.

        -Dpants.executable.path=/path/to/pants_dev/pants

* Remember to bootstrap pants in the project repository inside which you want to test the plugin.

        cd ~/workspace/example_project
        /path/to/pants_dev/pants goal goals

  This will bootstrap pants and resolve all the dependencies or else you will get an `ExecutionException` exception for exceeding 30s timeout.
