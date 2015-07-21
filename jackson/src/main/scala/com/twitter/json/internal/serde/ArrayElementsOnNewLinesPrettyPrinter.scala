package com.twitter.finatra.json.internal.serde

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter.Lf2SpacesIndenter

object ArrayElementsOnNewLinesPrettyPrinter extends DefaultPrettyPrinter {
  _arrayIndenter = Lf2SpacesIndenter.instance
}
