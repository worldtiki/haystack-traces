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
package com.expedia.www.haystack.trace.reader

import java.io.File

import com.codahale.metrics.JmxReporter
import com.expedia.www.haystack.commons.health.{HealthController, UpdateHealthStatusFile}
import com.expedia.www.haystack.commons.logger.LoggerUtils
import com.expedia.www.haystack.trace.reader.config.ProviderConfiguration
import com.expedia.www.haystack.trace.reader.metrics.MetricsSupport
import com.expedia.www.haystack.trace.reader.services.TraceService
import com.expedia.www.haystack.trace.reader.stores.CassandraEsTraceStore
import io.grpc.netty.NettyServerBuilder
import org.slf4j.{Logger, LoggerFactory}

object Service extends MetricsSupport {
  private val LOGGER: Logger = LoggerFactory.getLogger("TraceReader")

  // primary executor for service's async tasks
  implicit private val executor = scala.concurrent.ExecutionContext.global

  def main(args: Array[String]): Unit = {
    startJmxReporter()
    startService()
  }

  private def startJmxReporter() = {
    JmxReporter
      .forRegistry(metricRegistry)
      .build()
      .start()
  }

  private def startService(): Unit = {
    try {
      val config = new ProviderConfiguration
      HealthController.addListener(new UpdateHealthStatusFile(config.healthStatusFilePath))

      val store = new CassandraEsTraceStore(
        config.cassandraConfig,
        config.serviceMetadataConfig,
        config.elasticSearchConfig,
        config.indexConfig)(executor)

      val serviceConfig = config.serviceConfig

      val serverBuilder = NettyServerBuilder
        .forPort(serviceConfig.port)
        .directExecutor()
        .addService(new TraceService(store, config.traceValidatorConfig, config.traceTransformerConfig)(executor))

      // enable ssl if enabled
      if (serviceConfig.ssl.enabled) {
        serverBuilder.useTransportSecurity(new File(serviceConfig.ssl.certChainFilePath), new File(serviceConfig.ssl.privateKeyPath))
      }

      val server = serverBuilder.build().start()

      LOGGER.info(s"server started, listening on ${serviceConfig.port}")

      Runtime.getRuntime.addShutdownHook(new Thread() {
        override def run(): Unit = {
          LOGGER.info("shutting down gRPC server since JVM is shutting down")
          server.shutdown()
          store.close()
          LOGGER.info("server has been shutdown now")
        }
      })

      // mark the status of app as 'healthy'
      HealthController.setHealthy()

      server.awaitTermination()
    } catch {
      case ex: Throwable =>
        ex.printStackTrace()
        LOGGER.error("Fatal error observed while running the app", ex)
        LoggerUtils.shutdownLogger()
        System.exit(1)
    }
  }
}
