intellij-pants-plugin
==============

How to configure the project:
* Download and open IntelliJ IDEA 13 Community Edition and install Python Community Edition plugin
* Use IntelliJ IDEA 13 Community Edition as IDEA IC SDK (Project Structure(```Cmd + ;```) -> SDK -> '+' button -> IntelliJ Platform Plugin SDK)
* Add all jars from ~/Library/Application Support/IdeaIC13/python/lib/ to the sdk's classpath
* Run tests to verify your installation

Add sources to project:
* Clone the IntelliJ IDEA source here: https://github.com/JetBrains/intellij-community/
* Checkout sha a9946efb92325 (I recommend making a local branch for this)
* Go to the SDK configuration (see second bullet above), click Sourcepath, and add the directory you downloaded.

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
   
Useful links for contributors:
* IDEA Architecture http://confluence.jetbrains.com/display/IDEADEV/IntelliJ+IDEA+Architectural+Overview
* Plugin development http://confluence.jetbrains.com/display/IDEADEV/PluginDevelopment
