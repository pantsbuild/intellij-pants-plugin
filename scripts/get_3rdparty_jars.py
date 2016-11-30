import subprocess

output = subprocess.check_output("./pants dependencies : 3rdparty/intellij:: | grep 'com\|org\|jdk'", shell=True)

for l in output.splitlines():
  org, package, _ = l.split(':')
  # print org, package
  print "exclude('{}', '{}'),".format(org, package)