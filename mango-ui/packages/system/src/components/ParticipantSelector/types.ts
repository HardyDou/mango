export type ParticipantType = 'USER' | 'ORG' | 'ROLE' | 'POST';

export interface ParticipantSelectorValue {
  userIds?: string[];
  orgIds?: string[];
  roleIds?: string[];
  postIds?: string[];
}

export interface ParticipantTargetOption {
  label: string;
  value: string;
}

export interface ParticipantOrgTreeOption extends ParticipantTargetOption {
  children?: ParticipantOrgTreeOption[];
}

export interface ParticipantSelectorLoading {
  users?: boolean;
  roles?: boolean;
  posts?: boolean;
  orgs?: boolean;
}

