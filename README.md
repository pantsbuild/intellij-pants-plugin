intellij-pants-plugin
==============

How to configure the project:
* Download and open IntelliJ IDEA 13 Community Edition and install Python Community Edition plugin
* Use IntelliJ IDEA 13 Community Edition as IDEA IC sdk(Project Structure(Cmd + ;) -> SDK -> '+' button -> IntelliJ Platform Plugin SDK)
* Add all jars from ~/Library/Application Support/IdeaIC13/python/lib/ to the sdk's classpath
* Make sure that your project is set to configure bytecode compatible with 1.6  Preferences -> Compiler -> Java Compiler -> Project bytecode version
* Run tests to verify your installation


Contributing to the project:
* Checkout the code
   git clone https://github.com/pantsbuild/intellij-pants-plugin
* Create a new branch off master to make your changes
   git checkout -b $FEATURE_BRANCH
* Run tests to verify your installation
* Post your first review
   ./rbt post -o -g
* Iterating over the review
   ./rbt post -o -r <RB_ID>
* Commiting your change to master
   git checkout master
   git pull
   ./rbt patch -c <RB_ID>


Debugging the Plugin from local pants development:
* If you want to debug plugin using your local development pants, you can do so by using the property pants.executable.path.
  Add this configuration to Pants Run config.
  e.g.
  -Dpants.executable.pants=/path/to/pants_dev/pants
* Remember to bootstap pants in the project repository inside which you want to test the plugin.
  cd ~/workspace/example_project
  /path/to/pants_dev/pants goal goals

  This will bootstrap pants and resolve all the dependencies or else you will get an ExecutionException exception for exceeding 30s Timeout.
