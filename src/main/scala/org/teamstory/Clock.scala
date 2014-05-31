package org.teamstory

import org.joda.time.Instant

trait Clock {
    def now():Instant
}