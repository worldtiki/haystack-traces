/*
 *  Copyright 2017 Expedia, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.expedia.www.haystack.trace.indexer.writers.es

import io.searchbox.action.BulkableAction
import io.searchbox.core.{Bulk, DocumentResult}

/**
  * this is a thread safe builder to build index actions
  */
class ThreadSafeBulkBuilder(maxDocuments: Int, maxDocSizeInBytes: Int) {
  private var bulkBuilder = new Bulk.Builder()
  private var docsCount = 0
  private var totalSizeInBytes = 0

  /**
    * add the action in the bulk builder, returns bulk if any of the following condition is true
    * a) the total doc count in bulk is more than allowed setting
    * b) total size of the docs in bulk is more than allowed setting
    * c) force create the bulk
    * @param action index action
    * @param sizeInBytes total size of the json in the index action
    * @param forceBulkCreate force to build the existing bulk
    * @return
    */
  def addAction(action: BulkableAction[DocumentResult],
                sizeInBytes: Int,
                forceBulkCreate: Boolean): Option[Bulk] = {
    this.synchronized {
      bulkBuilder.addAction(action)
      docsCount += 1
      totalSizeInBytes += sizeInBytes

      if (forceBulkCreate ||
        docsCount >= maxDocuments ||
        totalSizeInBytes >= maxDocSizeInBytes) {
        Some(buildAndReset)
      } else {
        None
      }
    }
  }

  private def buildAndReset: Bulk = {
    val bulk = bulkBuilder.build()
    bulkBuilder = new Bulk.Builder
    docsCount = 0
    totalSizeInBytes = 0
    bulk
  }

  def getDocsCount: Int = docsCount
  def getTotalSizeInBytes: Int = totalSizeInBytes
}
