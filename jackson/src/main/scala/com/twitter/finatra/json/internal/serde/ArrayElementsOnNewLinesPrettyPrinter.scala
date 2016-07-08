package com.twitter.finatra.json.internal.serde

import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter

private[finatra] object ArrayElementsOnNewLinesPrettyPrinter extends DefaultPrettyPrinter {
  _arrayIndenter = DefaultIndenter.SYSTEM_LINEFEED_INSTANCE
}
