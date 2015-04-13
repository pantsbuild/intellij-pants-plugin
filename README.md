# intellij-pants-plugin

The intellij-pants-plugin supports importing, compiling and testing [pants](http://pantsbuild.github.io/) projects.
Current 1.* version of plugin supports Scala and Java projects only.

## User documentation

### Installing the Plugin
Please use “Plugins” tab: (Main menu: Settings | Plugins) to install the plugin.
Find "Pants Support" plugin. Install and Restart IntelliJ.

### Importing a Pants Project.
Using this plugin you can import entire project or specific targets in a BUILD file.

* Importing an entire project directory
    1. Use Main menu: File -> Import Project(in IJ 14.1+: File -> New -> Project From Existing Sources)
    2. Select project directory
       ![Import project from directory](images/import_dir1.png)
    3. Choose "Pants" on the next screen
    4. Make sure the check box "All Targets in the directory" is enabled and proceed with the wizard

* Importing targets from a BUILD File
    1. Use Main menu: File -> Import Project(in IJ 14.1+: File -> New -> Project From Existing Sources)
    2. Select a Build File from within the project
       ![Import project from BUILD file](images/import_file1.png)
    3. Check the targets you want to Import and proceed with the wizard. (Please wait for the targets to show up)
       ![Choose Targets](images/import_file2.png)
       
* Importing several BUILD files/directories(works in IntelliJ 14.1+)
    1. Import the first directory/BUILD file
    2. Use File -> New -> Module From Existing Sources to import next directories/BUILD files

Once you import the project using above steps, you will see the "Project View" with multiple modules configured.

### How the plugin works

The plugin uses `pants resolve export <list of imported targets>` command to get an information
about an imported project. `pants resolve export <list of imported targets>` command returns information
about all targets that are needed to be imported for the project in JSON format. It contains information about all dependencies of a target
as well as the same information for each dependency. Then the plugin creates an IntelliJ module for each target, configures
dependencies(modules and libraries) and source roots.

Let's check an output of `./pants export examples/src/java/org/pantsbuild/example/hello/main:main-bin` command:

```json
{
    "libraries": {},
    "targets": {
        "examples/src/java/org/pantsbuild/example/hello/greet:greet": {
            "is_code_gen": false,
            "target_type": "SOURCE",
            "libraries": [],
            "pants_target_type": "java_library",
            "targets": [],
            "roots": [
                {
                    "source_root": "/Users/fkorotkov/workspace/pants/examples/src/java/org/pantsbuild/example/hello/greet",
                    "package_prefix": "org.pantsbuild.example.hello.greet"
                }
            ]
        },
        "examples/src/java/org/pantsbuild/example/hello/main:main-bin": {
            "is_code_gen": false,
            "target_type": "SOURCE",
            "libraries": [],
            "pants_target_type": "jvm_binary",
            "targets": [
                "examples/src/java/org/pantsbuild/example/hello/greet:greet",
                "examples/src/resources/org/pantsbuild/example/hello:hello"
            ],
            "roots": [
                {
                    "source_root": "/Users/fkorotkov/workspace/pants/examples/src/java/org/pantsbuild/example/hello/main",
                    "package_prefix": "org.pantsbuild.example.hello.main"
                }
            ]
        },
        "examples/src/resources/org/pantsbuild/example/hello:hello": {
            "is_code_gen": false,
            "target_type": "RESOURCE",
            "libraries": [],
            "pants_target_type": "resources",
            "targets": [],
            "roots": [
                {
                    "source_root": "/Users/fkorotkov/workspace/pants/examples/src/resources/org/pantsbuild/example/hello",
                    "package_prefix": "org.pantsbuild.example.hello"
                }
            ]
        }
    }
}
```

The plugin will create three modules. One for the imported target, examples/src/java/com/pants/examples/hello/main:main-bin
and two for the targets it depends on. It also will configure source roots for the modules and will use `target_type`
and `is_code_gen` fields to figure out types of source roots(there are several types of source roots: sources,
test sources, resources, test resources, generated sources, etc).

### Compilation
The plugin provides two ways to compile your project:
* via pants' compile goal (Default)
  The plugin will use `pants compile <list of targets>` to compile your project once a `Make` command is invoked.
  ![Compilation via compile goal ](images/compilation_via_compile_goal.png)
* via IntelliJ's scala/java compilers
  Because the plugin configured all modules' dependencies IntelliJ can use this information to build and run your project without
  invoking pants. Using just internal representation of the project's model. We recommend to use first option to be consistent with
  command line invocation.

Compilation options can be configured in Preferences -> Build, Execution, Deployment -> Compiler -> Pants:
![Compilation Options](images/compilation_options.png)

### Plugin Features.
* Project File Tree View.
  The plugin configures modules per pants build target. Due to multiple modules, the default "Project View" is not very user friendly.
  The Plugin provides a custom view "Project Files Tree View". This view adheres to your repository file hierarchy.
  You can switch to this view as follows:
  ![Project Files Tree View](images/project_files_tree_view.png)
* BUILD File code assistance
  The plugin provides auto completion for target names in a BUILD file such as `jar_library`, `scala_library`, etc.
  As well as completion for dependencies' addresses.
* Project Regeneration using IntelliJ Action.
  If you add a dependency to your project, you can re-resolve project using IntelliJ Action in background.
  Use Main Menu: Help -> Find Action or Short hand Cmd+Shift+A and select Action "Refresh all External Projects"
  Remember to check "Include non-menu actions"
  ![Refresh Project](images/refresh_action.png)
* Compiling within IntelliJ
* Running tests within IntelliJ
  You can right click on tests and run tests.

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

        IJ_VERSION="14.1" IJ_BUILD_NUMBER="141.177" ./scripts/download-ci-environment.sh
        ./scripts/run-tests-ci.sh

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
    * `~/Library/Application Support/IdeaIC14/Scala/lib/jps/*.jar`
    * `/Applications/IntelliJ IDEA 14 CE.app/Contents/plugins/gradle/lib/gradle.jar`
* Set Scala 2.11.2 as your Scala SDK
* Make sure that your project is set to configure bytecode compatible with 1.6.  Preferences -> Compiler -> Java Compiler -> Project bytecode version
* Setup test dependencies
  * Some of the tests depend on local OSS install.
  * Checkout OSS pants in an adjacent directory to the plugin clone
* Run tests to verify your installation

### Debugging the Plugin from local pants development:

* If you want to debug plugin using your local development pants, you can do so by using the property `pants.executable.path`.
  Add this configuration to Pants Run config.
  e.g.

        -Dpants.executable.path=/path/to/pants_dev/pants

* To debug JPS compiler use:

        -Dcompiler.process.debug.port=PORT        

* Remember to bootstrap pants in the project repository inside which you want to test the plugin.

        cd ~/workspace/example_project
        /path/to/pants_dev/pants goals

  This will bootstrap pants and resolve all the dependencies or else you will get an `ExecutionException` exception for exceeding 30s timeout.

# Running tests with Pants

* Use `./scripts/setup-ci-environment.sh` with the latest `IJ_VERSION` and `IJ_BUILD_NUMBER` 
  environment variables from `.travis.ci` file to load everything for running tests. For example:
  
        IJ_VERSION="14.1" IJ_BUILD_NUMBER="141.177" ./scripts/setup-ci-environment.sh
        
* Execute `./scripts/run-tests-ci.sh` from command-line.
