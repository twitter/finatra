package com.twitter.inject.app.tests;

import java.lang.annotation.Annotation;

/** Work-around for https://github.com/scala/scala-dev/issues/249 */
public final class Annotations {
  private Annotations() {
  }

  public static final Down down() {
    return new Down() {
        public int hashCode() {
          // This is specified in java.lang.Annotation.
          return 127 * "value".hashCode();
        }

        /** Down specific equals */
        public boolean equals(Object o) {
          if (!(o instanceof Down)) {
            return false;
          }

          return true;
        }

        public String toString() {
          return "@" + Down.class.getName();
        }

        public Class<? extends Annotation> annotationType() {
          return Down.class;
        }

        private static final long serialVersionUID = 0;
      };
  }

  public static final Up up() {
    return new Up() {
      public int hashCode() {
        // This is specified in java.lang.Annotation.
        return 127 * "value".hashCode();
      }

      /** DUpown specific equals */
      public boolean equals(Object o) {
        if (!(o instanceof Up)) {
          return false;
        }

        return true;
      }

      public String toString() {
        return "@" + Up.class.getName();
      }

      public Class<? extends Annotation> annotationType() {
        return Up.class;
      }

      private static final long serialVersionUID = 0;
    };
  }
}
