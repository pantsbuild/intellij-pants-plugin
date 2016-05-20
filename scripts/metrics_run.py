from subprocess import call
import argparse
import csv
import os
import pandas as pd
import shutil
import logging

logging.basicConfig()
logger = logging.getLogger('metrics_app')
logger.setLevel(logging.DEBUG)

PROJECTS = [
  'examples/tests/java/org/pantsbuild/example/hello',
  'examples/src/scala/org/pantsbuild/example/hello/welcome/',
]

SCRIPT = './scripts/run-metrics.sh'

def convert_to_jvm_options(option_name, v):
  return "--jvm-test-junit-options=-D{}={}".format(option_name, v)


def main(args):
  error = False
  for k, v in vars(args).iteritems():
    if v is None:
      error = True
      logger.error("Error: {} is None".format(k))

  if error:
    exit(1)

  for project in PROJECTS:
    test_params = [
      convert_to_jvm_options("metricsImportDir", project),
      convert_to_jvm_options("metricsReportDir", args.report_dir),
    ]

    shutil.rmtree(args.report_dir)
    os.mkdir(args.report_dir)
    # logger.debug(test_params)
    # invoke metrics run with the following test params
    ret_code = call([SCRIPT] + test_params)
    if ret_code != 0:
      exit(ret_code)

    # the metric run will output csv files under the report directory
    # e.g. <report_dir>/resolve.csv, <report_dir>/indexing.csv,
    # which will be treated as 'resolve' and 'indexing' attribute with
    # their data.
    # each csv should only contain one data row, and we take its 'min'
    # attribute representing the time duration of interest.
    result = {}
    for filename in os.listdir(args.report_dir):
      if not filename.endswith('.csv'):
        continue
      attr = os.path.splitext(filename)[0]

      # sample csv looks like:
      # t, count, max, mean, min, stddev, p50, p75, p95, p98, p99, p999, mean_rate, m1_rate, m5_rate, m15_rate, rate_unit, duration_unit
      # 1463701922, 2, 7.442825, 7.442019, 7.441213, 0.000806, 7.442825, 7.442825, 7.442825, 7.442825, 7.442825, 7.442825, 0.026890, 0.011766, 0.005413, 0.002073, calls / second, seconds
      df = pd.read_csv(os.path.join(args.report_dir, filename))
      # take column 'min' and its 0th entry.
      result[attr] = df.loc[:, 'min'][0]


    logger.debug(result)



if __name__ == '__main__':
  parser = argparse.ArgumentParser()
  # parser.add_argument("--import-dir", help="project dir to import")
  parser.add_argument("--report-dir", help="report dir")
  main(parser.parse_args())
