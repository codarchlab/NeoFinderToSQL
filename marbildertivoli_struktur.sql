-- phpMyAdmin SQL Dump
-- version 3.3.9.2
-- http://www.phpmyadmin.net
--
-- Host: localhost
-- Erstellungszeit: 10. August 2015 um 13:22
-- Server Version: 5.5.9
-- PHP-Version: 5.3.5

SET SQL_MODE="NO_AUTO_VALUE_ON_ZERO";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;

--
-- Datenbank: `arachne`
--

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `marbildertivoli`
--

CREATE TABLE IF NOT EXISTS `marbildertivoli` (
  `PS_MarbilderTivoliID` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `FS_ArachneEntityID` bigint(20) unsigned DEFAULT NULL,
  `DateinameMarbildertivoli` varchar(255) CHARACTER SET utf8 DEFAULT NULL,
  `Dateiname` varchar(255) CHARACTER SET utf8 NOT NULL,
  `Pfad` varchar(255) CHARACTER SET utf8 NOT NULL,
  `Ordnertyp` varchar(255) CHARACTER SET utf8 NOT NULL,
  `erstellt` datetime DEFAULT NULL,
  `geaendert` datetime DEFAULT NULL,
  `Katalog` varchar(255) CHARACTER SET utf8 NOT NULL,
  `Volume` varchar(255) CHARACTER SET utf8 NOT NULL,
  `Dateityp` varchar(255) CHARACTER SET utf8 DEFAULT NULL COMMENT 'Typ nach CDFinder',
  PRIMARY KEY (`PS_MarbilderTivoliID`),
  KEY `FS_ArachneEntityID` (`FS_ArachneEntityID`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci AUTO_INCREMENT=53684900 ;
