/**
 * Copyright (c) 2013 Saddle Development Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/

// This allows us to utilize SBT to compile the jhdf5 library and deploy
// it to OSS Sonatype Repo.

import sbt._
import Keys._

object HDF5 extends sbt.Build {
  lazy val root =
    Project(id = "jhdf5",
            base = file("."),
            settings = Project.defaultSettings ++ Seq(
                scalaVersion := "2.9.2",
                crossScalaVersions := Seq("2.9.2", "2.10.0"),
                organization := "org.scala-saddle",
                publishMavenStyle := true,
                publishArtifact in Test := false,
                pomIncludeRepository := { x => false },
                pomExtra := (
                    <url>http://www.hdfgroup.org/hdf-java-html/</url>
                    <licenses>
                        <license>
                        <name>BSD style</name>
                        <url>http://www.hdfgroup.org/products/licenses.html</url>
                        <distribution>repo</distribution>
                        </license>
                    </licenses>
                    <scm>
                        <url>http://www.hdfgroup.org/ftp/HDF5/hdf-java/src/</url>
                    </scm>
                    <developers>
                      <developer>
                        <id>ncsa</id>
                        <name>NCSA</name>
                        <url>http://www.hdfgroup.org/HDF5/</url>
                      </developer>
                    </developers>
                ),
                resolvers ++= Seq(
                    "Sonatype OSS Releases" at "http://oss.sonatype.org/content/repositories/releases/",
                    "Sonatype OSS Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"
                ),
                version := "2.9",
                publishTo <<= (version) { version: String =>
                    val nexus = "https://oss.sonatype.org/"
                    if (version.trim.endsWith("SNAPSHOT"))
                        Some("snapshots" at nexus + "content/repositories/snapshots")
                    else
                        Some("releases" at nexus + "service/local/staging/deploy/maven2")
                },
                credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
            ))
}
