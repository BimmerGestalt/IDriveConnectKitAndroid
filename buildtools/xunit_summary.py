#!/usr/bin/env python

import xunitparser
import sys

colors = {
	'red': u'\u001b[31m',
	'red': u'\u001b[31m',
	'green': u'\u001b[32m',
	'yellow': u'\u001b[33m',
	'reset': u'\u001b[0m',
}
characters = {
	'ellipsis': u'\u2026',
	'checkmark': u'\u2714',
	'exclamation': u'\u2757',
	'cancel': u'\u2717',
}

ts, tr = xunitparser.parse(open(sys.argv[1]))

summary = {
	'good': 0,
	'skip': 0,
	'fail': 0,
	'total': 0,
}

print("------------------------------")
print("")
for tc in ts:
	summary['total'] += 1
	if tc.success:
		result = colors['green'] + characters['checkmark']
		summary['good'] += 1
	elif tc.skipped:
		result = colors['yellow'] + characters['ellipsis']
		summary['skip'] += 1
	elif tc.errored:
		result = colors['red'] + characters['exclamation']
		summary['fail'] += 1
	else:
		result = colors['red'] + characters['cancel']
		summary['fail'] += 1
	result = result + colors['reset']

	name = "%s.%s" % (tc.classname.split('.')[-1], tc.methodname)
	print("    %s %s  %.2fs" % (result, name, tc.time.microseconds/1000000.0))

print("")
print("------------------------------")
print("")
print("  %3d test cases in %.2fs" % (summary['total'], tr.time.microseconds/1000000.0))
print("%s  %3d passed%s" % (colors['green'], summary['good'], colors['reset']))
print("%s  %3d skipped%s" % (colors['yellow'], summary['skip'], colors['reset']))
print("%s  %3d failed%s" % (colors['red'], summary['fail'], colors['reset']))
print("")
print("------------------------------")
