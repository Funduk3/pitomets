package com.pitomets.monolit.components

import com.pitomets.monolit.repository.AnimalTypeRepository
import com.pitomets.monolit.repository.MetroRepository
import com.pitomets.monolit.service.ImportService
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.dao.DataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

@Component
class DbInitRunner(
    private val jdbcTemplate: JdbcTemplate,
    private val importService: ImportService,
    private val metroRepository: MetroRepository,
    private val animalTypeRepository: AnimalTypeRepository,
) : ApplicationRunner {
    @Suppress("LongMethod")
    override fun run(args: ApplicationArguments) {
        backfillUserApproval()
        backfillSellerProfileApproval()
        if (!tableExists("regions")) {
            importService.importFromFileRegion("data/regions.txt")
        }
        if (!tableExists("cities")) {
            importService.importFromFileCity("data/cities.txt")
        }
        if (!tableExists("metro_lines")) {
            importService.importFromFileMetro(
                path = "data/metro/metro_moscow.txt",
                requireNotNull(METRO_LINES["moscow"])
            )
            importService.importFromFileMetro(
                path = "data/metro/metro_spb.txt",
                requireNotNull(METRO_LINES["spb"])
            )
            importService.importFromFileMetro(
                path = "data/metro/metro_nizh_novgorod.txt",
                requireNotNull(METRO_LINES["nizh_novgorod"])
            )
            importService.importFromFileMetro(
                path = "data/metro/metro_kazan.txt",
                requireNotNull(METRO_LINES["kazan"])
            )
            importService.importFromFileMetro(
                path = "data/metro/metro_novosibirsk.txt",
                requireNotNull(METRO_LINES["novosibirsk"])
            )
            importService.importFromFileMetro(
                path = "data/metro/metro_ekb.txt",
                requireNotNull(METRO_LINES["ekaterinburg"])
            )
            importService.importFromFileMetro(
                path = "data/metro/metro_samara.txt",
                requireNotNull(METRO_LINES["samara"])
            )
            updateAllMetroLineColors()
        }
        if (!tableExists("animal_type")) {
            importService.importFromFileAnimalType("data/animal/animal_type.txt")
        }
        if (!tableExists("breed")) {
            val catType = checkNotNull(
                animalTypeRepository
                    .findByTitle("Кошки")
            ) {
                "Нет типа животного Кошки"
            }
            val dogType = checkNotNull(
                animalTypeRepository
                    .findByTitle("Собаки")
            ) {
                "Нет типа животного Собаки"
            }
            importService.importFromFileBreed(
                "data/animal/animal_breed/cat.txt",
                catType
            )
            importService.importFromFileBreed(
                "data/animal/animal_breed/dog.txt",
                dogType
            )
        }
    }

    private fun tableExists(tableName: String): Boolean {
        return try {
            jdbcTemplate.queryForObject("SELECT 1 FROM $tableName LIMIT 1", Int::class.java)
            true
        } catch (_: DataAccessException) {
            false
        }
    }

    private fun updateAllMetroLineColors() {
        val allLines = metroRepository.findAll()

        allLines.forEach { line ->
            val hexColor = getHexColorForLine(line.cityId, line.color)
            line.color = hexColor
        }

        metroRepository.saveAll(allLines)
    }

    private fun backfillUserApproval() {
        try {
            jdbcTemplate.update("UPDATE users SET is_approved = false WHERE is_approved IS NULL")
        } catch (_: DataAccessException) {
            // Table or column might not exist yet in a fresh DB
        }
    }

    private fun backfillSellerProfileApproval() {
        try {
            jdbcTemplate.update("UPDATE seller_profiles SET is_approved = false WHERE is_approved IS NULL")
        } catch (_: DataAccessException) {
            // Table or column might not exist yet in a fresh DB
        }
    }

    private fun getHexColorForLine(cityId: Long, colorName: String): String {
        val cityColors = requireNotNull(COLOR_MAPPINGS[cityId.toInt()])
        return requireNotNull(cityColors[colorName]) {
            log.info("SOS {}, {}", colorName, cityId)
        }
    }

    companion object {
        private val METRO_LINES = mapOf(
            "moscow" to 4L,
            "spb" to 14L,
            "nizh_novgorod" to 12L,
            "kazan" to 55L,
            "novosibirsk" to 13L,
            "ekaterinburg" to 33L,
            "samara" to 5L,
        )

        val COLOR_MAPPINGS = mapOf<Int, Map<String, String>>(
            4 to mapOf( // Москва
                "red" to "#E42313", // Сокольническая
                "brown" to "#915133", // Кольцевая
                "green" to "#4FB04F", // Замоскворецкая
                "darkblue" to "#0072BA", // Арбатско-Покровская
                "blue" to "#1EBCEF", // Филёвская
                "yellow" to "#FFCD1C", // Калининско-Солнцевская
                "violet" to "#943E90", // Таганско-Краснопресненская
                "orange" to "#F07E24", // Калужско-Рижская
                "gray" to "#ADACAC", // Серпуховско-Тимирязевская
                "lime" to "#BED12C", // Люблинско-Дмитровская
                "lightblue" to "#BAC8E8", // Бутовская
                "lightpink" to "#CC4C6E", // МЦК
                "cian" to "#88CDCF", // Большая кольцевая линия
                "hotpink" to "#CC0066", // Некрасовская
                "darkorange" to "#F5A528", // МЦД1
                "fuchsia" to "#E74683", // МЦД2
                "orangered" to "#EA5B04", // МЦД3
                "mediumaquamarine" to "#00CC66", // МЦД4
                "darkspringgreen" to "#03795F" // Троицкая
            ),
            14 to mapOf( // Санкт-Петербург
                "red" to "#D6083B", // Кировско-Выборгская
                "blue" to "#0078C9", // Московско-Петроградская
                "green" to "#009A49", // Невско-Василеостровская
                "yellow" to "#EA7125", // Правобережная
                "violet" to "#702785" // Фрунзенско-Приморская
            ),
            12 to mapOf( // Нижний Новгород
                "red" to "#D80707", // Автозаводская
                "blue" to "#0071BC" // Сормовско-Мещерская
            ),
            55 to mapOf( // Казань
                "red" to "#CD0505" // Центральная линия
            ),
            13 to mapOf( // Новосибирск
                "red" to "#CD0505", // Ленинская
                "green" to "#0A6F20" // Дзержинская
            ),
            33 to mapOf( // Екатеринбург
                "green" to "#009933" // Первая линия
            ),
            5 to mapOf( // Другой город
                "red" to "#CD0505" // Первая линия
            )
        )
        private val log = LoggerFactory.getLogger(DbInitRunner::class.java)
    }
    // https://api.superjob.ru/#metro_lines
}
