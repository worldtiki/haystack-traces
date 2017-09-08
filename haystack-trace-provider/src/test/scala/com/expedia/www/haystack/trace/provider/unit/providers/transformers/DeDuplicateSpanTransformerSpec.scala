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

package com.expedia.www.haystack.trace.provider.unit.providers.transformers

import com.expedia.open.tracing.Span
import com.expedia.www.haystack.trace.provider.providers.transformers.DeDuplicateSpanTransformer
import com.expedia.www.haystack.trace.provider.unit.BaseUnitTestSpec

class DeDuplicateSpanTransformerSpec extends BaseUnitTestSpec {

  describe("dedup span transformer") {
    it("should remove all the duplicate spans") {
      val span_1 = Span.newBuilder().setTraceId("traceId").setSpanId("span_1").build()
      val dup_span_1 = Span.newBuilder().setTraceId("traceId").setSpanId("span_1").build()

      val span_2 = Span.newBuilder().setTraceId("traceId").setSpanId("span_2").build()
      val dup_span_2 = Span.newBuilder().setTraceId("traceId").setSpanId("span_2").build()

      val transformer = new DeDuplicateSpanTransformer()
      var dedupSpans = transformer.transform(List(span_1, span_2, dup_span_2, dup_span_1))
      dedupSpans.size shouldBe 2
      dedupSpans.map(sp => sp.getSpanId) should contain allOf("span_1", "span_2")

      dedupSpans = transformer.transform(List(span_1, span_1, span_2, dup_span_2))
      dedupSpans.size shouldBe 2
      dedupSpans.map(sp => sp.getSpanId) should contain allOf("span_1", "span_2")
    }
  }
}
