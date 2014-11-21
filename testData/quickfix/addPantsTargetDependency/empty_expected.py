java_library(name='missingdepswhitelist2',
  sources=rglobs('*.java'), dependencies=['foo/bar/baz:tests'],
)