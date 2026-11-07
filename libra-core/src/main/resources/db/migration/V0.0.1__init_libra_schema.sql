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

create table if not exists "scopes" (
    id bigserial,
    tenant_id bigint not null,
    scope_name varchar(255) not null,
    created_at bigint not null,
    updated_at bigint not null,
    primary key (id),
    constraint fk__scopes_tenants_id foreign key (tenant_id) references tenants(id)
);
create index if not exists idx__scopes_tenant_id on scopes(tenant_id);
create index if not exists idx__scopes_scope_name on scopes(scope_name);

create table if not exists "policies" (
    id bigserial,
    tenant_id bigint not null,
    scope_id bigint not null,
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
    constraint fk__policies_scopes_id foreign key (scope_id) references scopes(id)
);
create index if not exists idx__policies_tenant_id on policies(tenant_id);
create index if not exists idx__policies_scope_id on policies(scope_id);
create index if not exists idx__policies_entity_id on policies(entity_id);
create index if not exists idx__policies_code on policies(code);

create table if not exists "policy_evaluations" (
    id bigserial,
    request_id varchar(255) not null,
    entity_id varchar(255) not null,
    policy_id bigint not null,
    input_attrs jsonb,
    result varchar(255) not null,
    errors jsonb,
    created_at bigint not null,
    updated_at bigint not null,
    primary key (id),
    constraint fk__policy_evaluations_policy_id foreign key (policy_id) references policies(id)
);
create index if not exists idx__policy_evaluations_request_id on policy_evaluations(request_id);
create index if not exists idx__policy_evaluations_entity_id on policy_evaluations(entity_id);
create index if not exists idx__policy_evaluations_policy_id on policy_evaluations(policy_id);

create or replace function set_timestamps() returns trigger
language plpgsql as $$
declare
now_ms bigint := (extract(epoch from now()) * 1000)::bigint;
begin
    if tg_op = 'INSERT' then
       new.created_at := coalesce(new.created_at, now_ms);
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

drop trigger if exists trg__scopes_timestamps on scopes;
create trigger trg__scopes_timestamps
    before insert or update on scopes
                         for each row execute function set_timestamps();

drop trigger if exists trg__policies_timestamps on policies;
create trigger trg__policies_timestamps
    before insert or update on policies
                         for each row execute function set_timestamps();

drop trigger if exists trg__policy_evaluations_timestamps on policy_evaluations;
create trigger trg__policy_evaluations_timestamps
    before insert or update on policy_evaluations
                         for each row execute function set_timestamps();
commit;