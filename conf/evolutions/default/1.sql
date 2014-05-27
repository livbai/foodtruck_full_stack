# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table citylocation (
  id                        varchar(255) not null,
  city_name                 varchar(255),
  zipcode                   varchar(255),
  state                     varchar(255),
  latitude                  double,
  longitude                 double,
  fetchtime                 bigint,
  constraint pk_citylocation primary key (id))
;

create table foodtruckinfo (
  id                        varchar(255) not null,
  applicant                 varchar(255),
  fooditems                 clob,
  address                   varchar(255),
  latitude                  double,
  longitude                 double,
  status                    varchar(255),
  fetchtime                 bigint,
  constraint pk_foodtruckinfo primary key (id))
;

create sequence citylocation_seq;

create sequence foodtruckinfo_seq;




# --- !Downs

SET REFERENTIAL_INTEGRITY FALSE;

drop table if exists citylocation;

drop table if exists foodtruckinfo;

SET REFERENTIAL_INTEGRITY TRUE;

drop sequence if exists citylocation_seq;

drop sequence if exists foodtruckinfo_seq;

