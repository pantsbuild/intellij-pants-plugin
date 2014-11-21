java_library(name='test',
  dependencies = [
    'bar/baz1',
    'bar/baz2',
  ],
  sources=rglobs('*.java'),
)