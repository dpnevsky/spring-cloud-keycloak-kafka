insert into cvs (id, uuid, name, surname, country_id, city, is_ready_to_relocate, is_ready_for_remote_work, status) values
    (1, 'c48741a2-df09-4c96-a42c-126c23335952', 'Ivan', 'Ivanov', 1, 'Moscow', false, true, 'DRAFT'),
    (2, 'c48741b3-df09-4c96-a42c-126c23335953', 'John', 'Smith', 2, 'New York', true, false, 'SUBMITTED');
alter table cvs alter column id restart with 3;
