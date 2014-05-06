package com.cj.etherlog

import org.joda.time.Instant

trait Clock {
    def now():Instant
}