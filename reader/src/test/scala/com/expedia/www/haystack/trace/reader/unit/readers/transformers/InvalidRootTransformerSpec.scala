/*
 *  Copyright 2017 Expedia, Inc.
 *
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *       Unless required by applicable law or agreed to in writing, software
 *       distributed under the License is distributed on an "AS IS" BASIS,
 *       WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *       See the License for the specific language governing permissions and
 *       limitations under the License.
 */

package com.expedia.www.haystack.trace.reader.unit.readers.transformers

import com.expedia.open.tracing.Span
import com.expedia.www.haystack.trace.reader.readers.transformers.InvalidRootTransformer
import com.expedia.www.haystack.trace.reader.unit.BaseUnitTestSpec

class InvalidRootTransformerSpec extends BaseUnitTestSpec {
  describe("InvalidRootTransformer") {
    it("should mark first span as root when there are multiple roots") {
      Given("trace with multiple roots ")
      val spans = List(
        Span.newBuilder()
          .setSpanId("a")
          .setStartTime(150000000000l + 300)
          .build(),
        Span.newBuilder()
          .setSpanId("b")
          .setStartTime(150000000000l)
          .build(),
        Span.newBuilder()
          .setSpanId("c")
          .setStartTime(150000000000l + 150)
          .build()
      )

      When("invoking transform")
      val transformedSpans = new InvalidRootTransformer().transform(spans)

      Then("pick first span as root and mark second's parent to be root")
      transformedSpans.length should be(3)

      val root = transformedSpans.filter(_.getParentSpanId.isEmpty)
      root.size should be(1)
      root.head.getSpanId should be("b")

      val others = transformedSpans.filter(!_.getParentSpanId.isEmpty)
      others.head.getParentSpanId should be("b")
      others.tail.head.getParentSpanId should be("b")
    }

    it("should mark first span as root when there are no roots") {
      Given("trace with multiple roots ")
      val spans = List(
        Span.newBuilder()
          .setSpanId("a")
          .setParentSpanId("x")
          .setStartTime(150000000000l + 300)
          .build(),
        Span.newBuilder()
          .setSpanId("b")
          .setParentSpanId("x")
          .setStartTime(150000000000l)
          .build(),
        Span.newBuilder()
          .setSpanId("c")
          .setParentSpanId("x")
          .setStartTime(150000000000l + 150)
          .build()
      )

      When("invoking transform")
      val transformedSpans = new InvalidRootTransformer().transform(spans)

      Then("pick first span as root and mark second's parent to be root")
      transformedSpans.length should be(3)

      val root = transformedSpans.filter(_.getParentSpanId.isEmpty)
      root.size should be(1)
      root.head.getSpanId should be("b")
    }

    it("should mark loopback span as root when there are no roots") {
      Given("trace with multiple roots ")
      val spans = List(
        Span.newBuilder()
          .setSpanId("a")
          .setParentSpanId("x")
          .setStartTime(150000000000l + 300)
          .build(),
        Span.newBuilder()
          .setSpanId("b")
          .setParentSpanId("x")
          .setStartTime(150000000000l)
          .build(),
        Span.newBuilder()
          .setSpanId("c")
          .setParentSpanId("c")
          .setStartTime(150000000000l + 150)
          .build()
      )

      When("invoking transform")
      val transformedSpans = new InvalidRootTransformer().transform(spans)

      Then("pick first span as root and mark second's parent to be root")
      transformedSpans.length should be(3)

      val root = transformedSpans.filter(_.getParentSpanId.isEmpty)
      root.size should be(1)
      root.head.getSpanId should be("c")
    }
  }
}
