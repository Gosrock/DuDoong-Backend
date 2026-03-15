package band.gosrock

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@EnableBatchProcessing
@SpringBootApplication
class BatchApplication

fun main(args: Array<String>) {
    val context = SpringApplication.run(BatchApplication::class.java, *args)
    System.exit(SpringApplication.exit(context))
}
