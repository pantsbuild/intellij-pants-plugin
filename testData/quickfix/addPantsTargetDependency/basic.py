java_library(name='test',
  dependencies = [
    'bar/baz1',
    'bar/baz2',
    'foo:scala',
  ],
  sources=rglobs('*.java'),
)