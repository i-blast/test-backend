alter table budget
    add column author_id integer null,
    add constraint fk_budget_author foreign key (author_id) references author (id) on delete set null;