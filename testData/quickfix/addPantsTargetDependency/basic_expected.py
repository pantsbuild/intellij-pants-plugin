java_library(name='test',
  dependencies = [
    'bar/baz1',
    'bar/baz2',
    'bar/baz3:baz3',
    'foo:scala',
  ],
  sources=rglobs('*.java'),
)