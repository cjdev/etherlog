package com.cj.etherlog.chart

import org.joda.time.Instant

case class ChartProjection (from:Instant, pointsRemaining:Int, whenComplete:Instant)