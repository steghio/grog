--DB management
create table aee(id int AUTO_INCREMENT, ip varchar(15) unique not null, primary key(id));
create table cc(id int AUTO_INCREMENT, ip varchar(15) unique not null, primary key(id));
create table rr(id int AUTO_INCREMENT, ip varchar(15) unique not null, primary key(id));
create table apps(id int AUTO_INCREMENT, appID varchar(50) not null, location varchar(200) not null, unique index(appID, location), primary key(id));