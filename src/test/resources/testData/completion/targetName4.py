jar_library(name='bar')
jar_library(name='baz')
jar_library(
    name='bin',
    dependencies=[
        ':b<caret>'
    ]
)