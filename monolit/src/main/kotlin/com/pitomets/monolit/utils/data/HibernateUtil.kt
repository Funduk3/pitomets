package com.pitomets.monolit.utils.data

import com.pitomets.monolit.model.entity.CityEntity
import org.hibernate.SessionFactory
import org.hibernate.cfg.Configuration

object HibernateUtil {
    val sessionFactory: SessionFactory = Configuration()
        .configure()
        .addAnnotatedClass(CityEntity::class.java)
        .buildSessionFactory()
}
