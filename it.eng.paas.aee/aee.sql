--DB aee
create table sandboxes(id int AUTO_INCREMENT, sID varchar(50) unique not null, appID varchar(50) not null, consolePort varchar(5), configDir varchar(200) not null, logFile varchar(200) not null, errFile varchar(200) not null, PID varchar(4), clientPort varchar(5), primary key(id));