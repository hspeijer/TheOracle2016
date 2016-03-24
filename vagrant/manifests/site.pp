$repo_root="/home/vagrant"

#notify{"Repo root is at : ${repo_root}": }
#include staging

class { 'staging':
  path  => "${repo_root}",
  owner => 'vagrant',
  group => 'vagrant',
}

#staging::extract { '${repo_root}/neo4j/neo4j-${neo4j_version}-unix.tar.gz':
#  target  => "${repo_root}/neo4j",
#  creates => '/tmp/staging/sample',
##  require => Staging::File["${repo_root}/neo4j/neo4j-enterprise-2.0.1-unix.tar.gz"],
#}

#exec{'start-neo4j':
#  command => "${repo_root}/neo4j/neo4j-${neo4j_version}/bin/neo4j start",
#  path => "${repo_root}/neo4j",
#}

#TODO Change neo4j server properties to use 0.0.0.0
#TODO Neo4j startup not working
#TODO Java version not working correctly on base image

