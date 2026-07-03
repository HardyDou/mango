create table flyway_comparison_data (
    id bigint primary key,
    data_code varchar(64) not null,
    data_name varchar(128) not null,
    data_value int not null
);

insert into flyway_comparison_data (id, data_code, data_name, data_value) values
    (1, 'TC184-OLD-001', '旧模式对比数据-1', 101),
    (2, 'TC184-OLD-002', '旧模式对比数据-2', 102),
    (3, 'TC184-OLD-003', '旧模式对比数据-3', 103),
    (4, 'TC184-OLD-004', '旧模式对比数据-4', 104),
    (5, 'TC184-OLD-005', '旧模式对比数据-5', 105);
