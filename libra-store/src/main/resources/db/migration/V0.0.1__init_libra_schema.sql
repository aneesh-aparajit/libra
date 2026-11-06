begin;
create table if not exists "tenants" (
    id bigserial,
    tenant_code varchar(255) not null,
    tenant_name varchar(255) not null,
    created_at bigint not null,
    updated_at bigint not null,
    primary key(id)
);
create index  if not exists idx__tenants_tenant_code on tenants(tenant_code);

create table if not exists "domains" (
    id bigserial,
    tenant_id bigint not null,
    domain_name varchar(255) not null,
    created_at bigint not null,
    updated_at bigint not null,
    primary key (id),
    constraint fk__domains_tenants_id foreign key (tenant_id) references tenants(id)
);
create index if not exists idx__domains_tenant_id on domains(tenant_id);
create index if not exists idx__domains_domain_name on domains(domain_name);

create table if not exists "policies" (
    id bigserial,
    tenant_id bigint not null,
    domain_id bigint not null,
    entity_id varchar(255) not null,
    code varchar(255) not  null,
    applied_js text,
    evaluate_js text not null,
    status varchar(255) not null,
    valid_from bigint not null,
    valid_till bigint,
    version int not null,
    created_at bigint not null,
    updated_at bigint not null,
    primary key (id),
    constraint fk__policies_tenants_id foreign key (tenant_id) references tenants(id),
    constraint fk__policies_domains_id foreign key (domain_id) references domains(id)
);
create index if not exists idx__policies_tenant_id on policies(tenant_id);
create index if not exists idx__policies_domain_id on policies(domain_id);
create index if not exists idx__policies_entity_id on policies(entity_id);
create index if not exists idx__policies_code on policies(code);

create table if not exists "executions" (
    id bigserial,
    request_id varchar(255) not null,
    entity_id varchar(255) not null,
    policy_id bigint not null,
    input_attrs jsonb,
    response varchar(255) not null,
    errors jsonb,
    created_at bigint not null,
    updated_at bigint not null,
    primary key (id),
    constraint fk__executions_policy_id foreign key (policy_id) references policies(id)
);
create index if not exists idx__executions_request_id on executions(request_id);
create index if not exists idx__executions_entity_id on executions(entity_id);
create index if not exists idx__executions_policy_id on executions(policy_id);

create or replace function set_timestamps() returns trigger
language plpgsql as $$
declare
    now_ms bigint := (extract(epoch from now()) * 1000)::bigint;
begin
    if tg_op = 'INSERT' then
       new.created_at := coalease(new.created_at, now_ms);
    else
        new.created_at := old.created_at;
    end if;
    new.updated_at := now_ms;
    return new;
end;
$$;

drop trigger if exists trg__tenants_timestamps on tenants;
create trigger trg__tenants_timestamps before insert or update on tenants
    for each row execute function set_timestamps();

drop trigger if exists trg__domains_timestamps on domains;
create trigger trg__domains_timestamps
    before insert or update on domains
                         for each row execute function set_timestamps();

drop trigger if exists trg__policies_timestamps on policies;
create trigger trg__policies_timestamps
    before insert or update on policies
                         for each row execute function set_timestamps();

drop trigger if exists trg__executions_timestamps on executions;
create trigger trg__executions_timestamps
    before insert or update on executions
                         for each row execute function set_timestamps();
commit;