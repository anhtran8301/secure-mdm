-- A timestamp can tie. An insertion sequence preserves FIFO even with a fixed test clock.
ALTER TABLE device_commands ADD queue_order BIGINT NOT NULL AUTO_INCREMENT UNIQUE;
CREATE INDEX command_fifo ON device_commands(organization_id,device_id,status,queue_order);
ALTER TABLE device_commands ADD CONSTRAINT command_creator_tenant FOREIGN KEY(organization_id,created_by) REFERENCES users(organization_id,id);
