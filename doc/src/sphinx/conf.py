# -*- coding: utf-8 -*-
#
# Documentation config
#

import sys, os, datetime

sys.path.append(os.path.abspath('exts'))
sys.path.append(os.path.abspath('utils'))

import sbt_versions

highlight_language = 'text'
extensions = [
  'sphinx.ext.extlinks',
  'sphinx.ext.autodoc',
  'sphinx.ext.intersphinx',
  'sphinx.ext.ifconfig',
  'includecode'
]
templates_path = ['_templates']
source_suffix = '.rst'
master_doc = 'index'
exclude_patterns = ['_build']

sys.path.append(os.path.abspath('_themes'))
html_theme_path = ['_themes']
html_theme = 'flask'
html_short_title = 'Finatra'
html_static_path = ['_static']
html_sidebars = {
    'index':    ['sidebarintro.html', 'searchbox.html'],
    '**':       ['sidebarintro.html', 'localtoc.html', 'relations.html', 'searchbox.html']
}
html_favicon = '_static/favicon.ico'
html_theme_options = {
    'index_logo': None
}
html_logo = "../../../../finatra_logo.png"
html_show_sphinx = False

project = 'Finatra'
description = """Fast, testable, Scala services built on TwitterServer and Finagle."""
copyright = '2013-{} Twitter, Inc'.format(datetime.datetime.now().year)
version = ''
release = ''
htmlhelp_basename = "finatra"
release = sbt_versions.find_release(os.path.abspath('../../../build.sbt'))
version = sbt_versions.release_to_version(release)

# e.g. :issue:`36` :ticket:`8`
extlinks = {
    'issue': ('https://github.com/twitter/finatra/issues/%s', 'issue #')
}

rst_epilog = '''
.. _Sphinx: https://www.sphinx-doc.org/en/master/
'''

pygments_style = 'sphinx'

# fall back if theme is not there
try:
    __import__('flask_theme_support')
except ImportError as e:
    print('-' * 74)
    print('Warning: Flask themes unavailable.  Building with default theme')
    print('If you want the Flask themes, run this command and build again:')
    print()
    print('  git submodule update --init')
    print('-' * 74)
    
    pygments_style = 'tango'
    html_theme = 'default'
    html_theme_options = {}
