# -*- mode: makefile-Gmake; -*-

.PHONY: all clean test test-sparse

SCRIPTS := scripts
SETUP_SCRIPT := $(SCRIPTS)/setup-ci-environment.sh
PREP_SRC := $(SCRIPTS)/prepare-ci-environment.sh
RUN_TESTS := $(SCRIPTS)/run-tests-ci.sh

bash_run_env = source $(PREP_SRC) && $(1)
bash_var = $(1) := $(shell $(call bash_run_env,echo "$$$(1)"))

TEST_CACHE_DIR := .cache

$(eval $(call bash_var,FULL_IJ_BUILD_NUMBER))
INTELLIJ_CACHE_DIR := $(TEST_CACHE_DIR)/intellij/$(FULL_IJ_BUILD_NUMBER)
JDK_CACHE_DIR := $(TEST_CACHE_DIR)/jdk-libs
INTELLIJ_IDEA_CACHE_DIR := $(INTELLIJ_CACHE_DIR)/idea-dist
INTELLIJ_PLUGIN_CACHE_DIR := $(INTELLIJ_CACHE_DIR)/plugins

JDK_JAR_NAMES := sa-jdi tools
INTELLIJ_PLUGIN_NAMES := Scala python
$(eval $(call bash_var,IJ_BUILD))
CACHE_FILES := \
	$(foreach base,$(JDK_JAR_NAMES),$(JDK_CACHE_DIR)/$(base).jar) \
	$(INTELLIJ_IDEA_CACHE_DIR)/idea$(IJ_BUILD).tar.gz \
	$(foreach p,$(INTELLIJ_PLUGIN_NAMES),$(INTELLIJ_PLUGIN_CACHE_DIR)/$(p).zip) \
	$(TEST_CACHE_DIR)/pants

PANTS_DIR := .pants.d

PANTS := ./pants
PANTS_INI := pants.ini

IVY_CACHE_DIR := ~/.ivy2
PANTS_USER_CACHE_DIR := ~/.cache/pants

# wish you could declare a dependency on removing files/directories as well in
# make, not just creating them (for example)
CACHE_DIRS := $(TEST_CACHE_DIR) $(PANTS_DIR) $(IVY_CACHE_DIR) \
	$(PANTS_USER_CACHE_DIR)

pants_run_env = $(call bash_run_env,$(PANTS) $(1))

PANTS_TARGETS := 'tests::'

all: $(CACHE_FILES) $(PANTS)
	$(call pants_run_env,compile $(PANTS_TARGETS))

$(CACHE_FILES): $(SETUP_SCRIPT) $(PREP_SRC)
	$(SETUP_SCRIPT)

$(PANTS): $(PANTS_INI)
	$(PANTS) -V

clean:
	rm -rf $(CACHE_DIRS)

test: all
	$(RUN_TESTS)

test-sparse: all
	$(RUN_TESTS) --fail-fast --test-junit-output-mode=FAILURE_ONLY
