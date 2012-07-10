#!/bin/sh
rsync -avz -e ssh _build/html/* capotej@ancomment.com:finatra_docs/

