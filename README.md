intellij-pants-plugin
==============

How to configure the project:
* Download and open IntelliJ IDEA 13 Community Edition and install Python Community Edition plugin
* Use IntelliJ IDEA 13 Community Edition as IDEA IC sdk(Project Structure(Cmd + ;) -> SDK -> '+' button -> IntelliJ Platform Plugin SDK)
* Add all jars from ~/Library/Application Support/IdeaIC13/python/lib/ to the sdk's classpath
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
