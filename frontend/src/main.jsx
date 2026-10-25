import React, { useEffect, useRef, useState } from "react";
import { createRoot } from "react-dom/client";
import {
  ArrowDown,
  ArrowUp,
  Bell,
  BookOpen,
  BriefcaseBusiness,
  Camera,
  CalendarDays,
  Cloud,
  Cpu,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  ClipboardList,
  Database,
  Eye,
  EyeOff,
  FileText,
  Folder,
  Globe2,
  HardDrive,
  Headphones,
  Home,
  KeyRound,
  Link2,
  LockKeyhole,
  LogOut,
  Mail,
  MapPin,
  MessageSquare,
  Monitor,
  Moon,
  Network,
  Pencil,
  Package,
  Phone,
  Plus,
  Printer,
  Search,
  Settings,
  Server,
  Shield,
  Smartphone,
  Sun,
  Trash2,
  UserCog,
  Users,
  Wifi,
  Wrench,
  X,
} from "lucide-react";
import "./style.css";
import "./layout-fix.css";

const API = "/it/api";
const fallbackSites = [
  {
    id: 1,
    siteCode: "work",
    name: "업무 관리",
    url: "/worklog",
    iconKey: "work",
    sortOrder: 10,
    external: false,
  },
  {
    id: 2,
    siteCode: "insa",
    name: "인사 관리",
    url: "/insa",
    iconKey: "users",
    sortOrder: 20,
    external: false,
  },
  {
    id: 3,
    siteCode: "ip",
    name: "IP 관리",
    url: "/ip",
    iconKey: "network",
    sortOrder: 30,
    external: false,
  },
  {
    id: 4,
    siteCode: "management",
    name: "자산 관리",
    url: "/management",
    iconKey: "database",
    sortOrder: 40,
    external: false,
  },
  {
    id: 5,
    siteCode: "register",
    name: "관리대장",
    url: "/register",
    iconKey: "clipboard",
    sortOrder: 50,
    external: false,
  },
];
const iconMap = {
  work: BriefcaseBusiness,
  users: Users,
  network: Network,
  database: Database,
  clipboard: ClipboardList,
  link: Link2,
  home: Home,
  calendar: CalendarDays,
  document: FileText,
  folder: Folder,
  globe: Globe2,
  storage: HardDrive,
  mail: Mail,
  monitor: Monitor,
  package: Package,
  server: Server,
  shield: Shield,
  tools: Wrench,
  book: BookOpen,
  bell: Bell,
  phone: Phone,
  smartphone: Smartphone,
  printer: Printer,
  camera: Camera,
  wifi: Wifi,
  cloud: Cloud,
  lock: LockKeyhole,
  key: KeyRound,
  search: Search,
  location: MapPin,
  message: MessageSquare,
  headset: Headphones,
  cpu: Cpu,
};
const iconChoices = [
  ["link", "링크"],
  ["home", "홈"],
  ["work", "업무"],
  ["users", "사용자"],
  ["network", "네트워크"],
  ["database", "데이터베이스"],
  ["clipboard", "대장"],
  ["calendar", "일정"],
  ["document", "문서"],
  ["folder", "폴더"],
  ["globe", "웹"],
  ["storage", "저장소"],
  ["mail", "메일"],
  ["monitor", "시스템"],
  ["package", "물품"],
  ["server", "서버"],
  ["shield", "보안"],
  ["tools", "도구"],
  ["book", "자료"],
  ["bell", "알림"],
  ["phone", "전화"],
  ["smartphone", "휴대전화"],
  ["printer", "프린터"],
  ["camera", "카메라"],
  ["wifi", "무선 네트워크"],
  ["cloud", "클라우드"],
  ["lock", "잠금"],
  ["key", "키"],
  ["search", "검색"],
  ["location", "위치"],
  ["message", "메시지"],
  ["headset", "헤드셋"],
  ["cpu", "프로세서"],
];
const defaultOrder = fallbackSites.map((site) => site.siteCode);
const defaultIconColors = Object.fromEntries(
  [...defaultOrder, "external", "settings", "admin", "logout"].map((key) => [key, "#2563eb"]),
);
const normalizeSiteUrl = (value) => {
  const url = value.trim();
  if (!url || url.startsWith("/") || /^https?:\/\//i.test(url)) return url;
  return `https://${url}`;
};

function Login({ onLogin }) {
  const [username, setUsername] = useState(""),
    [password, setPassword] = useState(""),
    [error, setError] = useState(""),
    [busy, setBusy] = useState(false);
  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError("");
    try {
      const r = await fetch(`${API}/auth/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username, password }),
      });
      if (!r.ok) throw new Error("아이디 또는 비밀번호를 확인해 주세요.");
      onLogin(await r.json());
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  };
  return (
    <main className="login-page">
      <section className="login-card">
        <h1>SRGH IT</h1>
        <form onSubmit={submit}>
          <label>
            아이디
            <input
              autoFocus
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="아이디를 입력하세요"
              required
            />
          </label>
          <label>
            비밀번호
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="비밀번호를 입력하세요"
              required
            />
          </label>
          {error && <p className="error">{error}</p>}
          <button disabled={busy}>{busy ? "로그인 중" : "로그인"}</button>
        </form>
        <p className="login-foot">SRGH IT · Internal Service</p>
      </section>
    </main>
  );
}

function SiteManagement({ sites, order, onMove, onChange, groups, onGroupsChange, showExternalGroup, onShowExternalGroupChange, iconColors, onIconColorChange, hiddenSiteCodes, onVisibilityChange, isAdmin }) {
  const empty = { name: "", url: "", iconKey: "link", iconColor: "#2563eb", external: false };
  const [draft, setDraft] = useState(empty),
    [editing, setEditing] = useState(null),
    [iconPickerOpen, setIconPickerOpen] = useState(false),
    [externalGroupOpen, setExternalGroupOpen] = useState(false),
    [openSettingsGroups, setOpenSettingsGroups] = useState({}),
    [groupName, setGroupName] = useState(""),
    [editingGroupId, setEditingGroupId] = useState(null),
    [urlCheck, setUrlCheck] = useState({
      checking: false,
      reachable: false,
      embeddable: false,
      normalizedUrl: "",
      message: "",
    }),
    [message, setMessage] = useState("");
  useEffect(() => {
    const url = draft.url.trim();
    if (!url) {
      setUrlCheck({
        checking: false,
        reachable: false,
        embeddable: false,
        normalizedUrl: "",
        message: "",
      });
      return;
    }
    const controller = new AbortController();
    setUrlCheck((value) => ({ ...value, checking: true, message: "주소 확인 중..." }));
    const timer = setTimeout(() => {
      fetch(`${API}/settings/site-check?url=${encodeURIComponent(url)}`, {
        signal: controller.signal,
      })
        .then((response) => (response.ok ? response.json() : Promise.reject()))
        .then((result) => setUrlCheck({ ...result, checking: false }))
        .catch((error) => {
          if (error.name !== "AbortError") {
            setUrlCheck({
              checking: false,
              reachable: false,
              embeddable: false,
              normalizedUrl: normalizeSiteUrl(url),
              message: "주소를 확인하지 못했습니다. 외부 사이트를 체크해 주세요.",
            });
          }
        });
    }, 450);
    return () => {
      clearTimeout(timer);
      controller.abort();
    };
  }, [draft.url]);
  const canSubmit =
    draft.name.trim() &&
    draft.url.trim() &&
    (String(editing?.id).startsWith("global:") || draft.external || (!urlCheck.checking && urlCheck.embeddable));
  const submit = async (e) => {
    e.preventDefault();
    if (!canSubmit) return;
    setMessage("");
    const uniqueId =
      globalThis.crypto?.randomUUID?.() ||
      `${Date.now()}-${Math.random().toString(36).slice(2, 9)}`;
    const customCode = `custom-${uniqueId}`;
    const normalizedDraft = {
      ...draft,
      url: urlCheck.normalizedUrl || normalizeSiteUrl(draft.url),
    };
    const { iconColor, ...siteDraft } = normalizedDraft;
    const saved = editing
      ? { ...editing, ...siteDraft,
          ...(String(editing.id).startsWith("global:") ? { name: editing.name, url: editing.url } : {}) }
      : {
          ...siteDraft,
          id: customCode,
          siteCode: customCode,
          sortOrder:
            Math.max(0, ...sites.map((site) => site.sortOrder || 0)) + 10,
        };
    onChange(
      [...sites.filter((site) => site.id !== saved.id), saved].sort(
        (a, b) => a.sortOrder - b.sortOrder || a.id - b.id,
      ),
      { [saved.siteCode]: iconColor },
    );
    setDraft(empty);
    setEditing(null);
    setMessage(editing ? "사이트를 수정했습니다." : "사이트를 추가했습니다.");
  };
  const edit = (site) => {
    setEditing(site);
    setDraft({
      name: site.name,
      url: site.url,
      iconKey: site.iconKey,
      iconColor: iconColors[site.siteCode] || "#2563eb",
      external: Boolean(site.external),
    });
  };
  const remove = async (site) => {
    if (!confirm(`${site.name} 사이트를 삭제하시겠습니까?`)) return;
    onChange(sites.filter((item) => item.id !== site.id));
    if (editing?.id === site.id) {
      setEditing(null);
      setDraft(empty);
    }
    setMessage("사이트를 삭제했습니다.");
  };
  const SelectedIcon = iconMap[draft.iconKey] || Link2;
  const orderedSites = [
    ...order.map((key) => sites.find((site) => site.siteCode === key)).filter(Boolean),
    ...sites.filter((site) => !order.includes(site.siteCode)),
  ];
  const assignedCodes = new Set(groups.flatMap((group) => group.siteCodes));
  const ungroupedSites = orderedSites.filter((site) => !assignedCodes.has(site.siteCode));
  const internalSites = ungroupedSites.filter((site) => !site.external);
  const externalSites = ungroupedSites.filter((site) => site.external);
  const setSiteGroup = (siteCode, groupId) => onGroupsChange(groups.map((group) => ({
    ...group,
    siteCodes: group.id === groupId
      ? [...group.siteCodes.filter((code) => code !== siteCode), siteCode]
      : group.siteCodes.filter((code) => code !== siteCode),
  })));
  const saveGroup = (event) => {
    event.preventDefault();
    const name = groupName.trim();
    if (!name) return;
    if (editingGroupId) {
      onGroupsChange(groups.map((group) => group.id === editingGroupId ? { ...group, name } : group));
    } else {
      const groupId = globalThis.crypto?.randomUUID?.() ||
        `${Date.now()}-${Math.random().toString(36).slice(2, 9)}`;
      onGroupsChange([...groups, { id: `group-${groupId}`, name, siteCodes: [], collapsed: true }]);
    }
    setGroupName("");
    setEditingGroupId(null);
  };
  const renderSiteRow = (site) => {
    const Icon = iconMap[site.iconKey] || Link2;
    const shared = String(site.id).startsWith("global:");
    const assignedGroup = groups.find((item) => item.siteCodes.includes(site.siteCode));
    const siblings = assignedGroup
      ? orderedSites.filter((item) => assignedGroup.siteCodes.includes(item.siteCode))
      : site.external && showExternalGroup ? externalSites : showExternalGroup ? internalSites : ungroupedSites;
    const position = siblings.findIndex((item) => item.siteCode === site.siteCode);
    return (
      <div className="site-row" key={site.id}>
        <i>
          <Icon style={{ color: iconColors[site.siteCode] || "#2563eb" }} />
        </i>
        <div>
          <strong>{site.name}</strong>
          <span>{site.url}</span>
        </div>
        <div className="site-order-controls">
          <button type="button" title={`${site.name} 위로`} aria-label={`${site.name} 위로`}
            disabled={position === 0} onClick={() => onMove(site.siteCode, siblings[position - 1].siteCode)}><ArrowUp /></button>
          <button type="button" title={`${site.name} 아래로`} aria-label={`${site.name} 아래로`}
            disabled={position === siblings.length - 1} onClick={() => onMove(site.siteCode, siblings[position + 1].siteCode)}><ArrowDown /></button>
        </div>
        <select className="site-group-select" aria-label={`${site.name} 폴더`} value={assignedGroup?.id || ""}
          onChange={(event) => setSiteGroup(site.siteCode, event.target.value)}>
          <option value="">{site.external && showExternalGroup ? "외부사이트" : "폴더 없음"}</option>
          {groups.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}
        </select>
        <button
          type="button"
          className={`site-visibility ${hiddenSiteCodes.includes(site.siteCode) ? "private" : ""}`}
          title={hiddenSiteCodes.includes(site.siteCode) ? "내 네비게이션에서 활성화" : "내 네비게이션에서 비활성화"}
          aria-label={`${site.name} ${hiddenSiteCodes.includes(site.siteCode) ? "비활성" : "활성"}`}
          onClick={() => onVisibilityChange(site.siteCode)}
        >
          {hiddenSiteCodes.includes(site.siteCode) ? "비활성" : "활성"}
        </button>
        <label className="site-row-color" title="아이콘 색상">
          <input
            type="color"
            value={iconColors[site.siteCode] || "#2563eb"}
            onChange={(e) => onIconColorChange(site.siteCode, e.target.value)}
          />
        </label>
        <button title={shared ? "아이콘과 외부 사이트 설정 수정" : "수정"} onClick={() => edit(site)}>
          <Pencil />
        </button>
        <button
          className="delete"
          title={shared ? "공통 사이트는 navigation_site에서 삭제합니다" : "삭제"}
          disabled={shared}
          onClick={() => remove(site)}
        >
          <Trash2 />
        </button>
      </div>
    );
  };
  return (
    <section className="panel site-management">
      <div className="site-management-title">
        <h3>사이트 설정</h3>
      </div>
      <form className={`site-form ${editing ? "editing-site" : ""}`} onSubmit={submit}>
        <label className="site-name-field">
          사이트명
          <input
            aria-label="사이트명"
            value={draft.name}
            onChange={(e) => setDraft({ ...draft, name: e.target.value })}
            readOnly={String(editing?.id).startsWith("global:")}
            placeholder="예: 전산 자료실"
            required
          />
          <span
            className={`site-check-slot ${draft.external || urlCheck.embeddable ? "valid" : "invalid"}`}
          >
            {draft.url.trim()
              ? draft.external && !urlCheck.embeddable
                ? "외부 사이트로 새 탭에서 열립니다."
                : urlCheck.message
              : "\u00a0"}
          </span>
        </label>
        <label>
          주소
          <input
            value={draft.url}
            onChange={(e) => setDraft({ ...draft, url: e.target.value })}
            readOnly={String(editing?.id).startsWith("global:")}
            placeholder="/path 또는 https://..."
            required
          />
        </label>
        <div className="icon-field">
          <span>아이콘</span>
          <button
            type="button"
            className="icon-select"
            title="아이콘 선택"
            aria-label="아이콘 선택"
            onClick={() => setIconPickerOpen(true)}
          >
            <SelectedIcon />
          </button>
        </div>
        <label className="site-color-field">
          색상
          <input
            type="color"
            value={draft.iconColor}
            onChange={(e) => setDraft({ ...draft, iconColor: e.target.value })}
            title="아이콘 색상"
          />
        </label>
        {editing && (
          <button
            type="button"
            className="site-cancel"
            onClick={() => {
              setEditing(null);
              setDraft(empty);
            }}
          >
            취소
          </button>
        )}
        <div className="site-save-field">
          <label className="external-check">
            <input
              type="checkbox"
              checked={draft.external}
              onChange={(e) =>
                setDraft({ ...draft, external: e.target.checked })
              }
            />
            <span>외부 사이트</span>
          </label>
          <button className="site-save" disabled={!canSubmit}>
            {editing ? <Pencil /> : <Plus />}
            {editing ? "수정 저장" : "사이트 추가"}
          </button>
        </div>
      </form>
      {message && <p className="message">{message}</p>}
      <form className="site-folder-form" onSubmit={saveGroup}>
        <label>폴더 이름<input value={groupName} maxLength={50} onChange={(event) => setGroupName(event.target.value)}
          placeholder="예: 자주 쓰는 사이트" required /></label>
        <button type="submit">{editingGroupId ? "이름 저장" : "폴더 추가"}</button>
        {editingGroupId && <button type="button" className="site-cancel" onClick={() => {
          setEditingGroupId(null); setGroupName("");
        }}>취소</button>}
      </form>
      <div className="site-list">
        {(showExternalGroup ? internalSites : ungroupedSites).map(renderSiteRow)}
        {groups.map((group) => {
          const members = orderedSites.filter((site) => group.siteCodes.includes(site.siteCode));
          const isOpen = Boolean(openSettingsGroups[group.id]);
          return <div className="site-group" key={group.id}>
            <div className="site-group-heading">
              <button type="button" className="site-group-toggle" aria-expanded={isOpen}
                onClick={() => setOpenSettingsGroups((current) => ({ ...current, [group.id]: !current[group.id] }))}>
                <Folder style={{ color: iconColors[group.id] || "#2563eb" }} />
                <strong>{group.name}</strong><span>{members.length}</span>
                <ChevronDown className={isOpen ? "open" : ""} />
              </button>
              <button type="button" aria-label={`${group.name} 이름 수정`} title="이름 수정" onClick={() => {
                setEditingGroupId(group.id); setGroupName(group.name);
              }}><Pencil /></button>
              <button type="button" aria-label={`${group.name} 폴더 삭제`} title="폴더 삭제" onClick={() => {
                if (confirm(`${group.name} 폴더를 삭제하시겠습니까? 사이트는 삭제되지 않습니다.`))
                  onGroupsChange(groups.filter((item) => item.id !== group.id));
              }}><Trash2 /></button>
            </div>
            {isOpen && <div className="site-group-items">{members.length
              ? members.map(renderSiteRow) : <span className="empty-folder">사이트를 폴더로 옮겨 주세요.</span>}</div>}
          </div>;
        })}
        <div className="site-group">
          <div className="external-group-heading">
            {showExternalGroup ? <button type="button" className="site-group-toggle"
              onClick={() => setExternalGroupOpen((open) => !open)}>
              <Globe2 style={{ color: iconColors.external || "#2563eb" }} />
              <strong>외부사이트</strong><span>{externalSites.length}</span>
              <ChevronDown className={externalGroupOpen ? "open" : ""} />
            </button> : <div className="site-group-toggle external-group-disabled">
              <Globe2 style={{ color: iconColors.external || "#2563eb" }} />
              <strong>외부사이트</strong><span>{externalSites.length}</span>
            </div>}
            <button type="button" className={`external-group-visibility ${showExternalGroup ? "" : "active"}`}
              aria-label={showExternalGroup ? "외부사이트 표시 안함" : "외부사이트 표시"}
              title={showExternalGroup ? "외부사이트 표시 안함" : "외부사이트 표시"}
              aria-pressed={!showExternalGroup} onClick={() => onShowExternalGroupChange(!showExternalGroup)}>
              {showExternalGroup ? <Eye /> : <EyeOff />}
            </button>
          </div>
            {showExternalGroup && externalGroupOpen && (
              <div className="site-group-items">
                {externalSites.map(renderSiteRow)}
              </div>
            )}
          </div>
      </div>
      <div className="fixed-icon-colors">
        <strong>메뉴 색상</strong>
        <div>
          {[
            ...(showExternalGroup ? [["external", "외부사이트", Globe2]] : []),
            ...groups.map((group) => [group.id, group.name, Folder]),
            ["settings", "설정", Settings],
            ...(isAdmin ? [["admin", "관리", UserCog]] : []),
            ["logout", "로그아웃", LogOut],
          ].map(([key, name, Icon]) => {
            const color = iconColors[key] || "#2563eb";
            return (
              <label key={key} className="fixed-color-row">
                <Icon style={{ color }} />
                <span>{name}</span>
                <input type="color" value={color} onChange={(e) => onIconColorChange(key, e.target.value)} />
              </label>
            );
          })}
        </div>
      </div>
      {iconPickerOpen && (
        <div
          className="modal-backdrop"
          onMouseDown={(e) =>
            e.target === e.currentTarget && setIconPickerOpen(false)
          }
        >
          <div className="modal icon-picker-modal">
            <button
              type="button"
              className="close"
              title="닫기"
              onClick={() => setIconPickerOpen(false)}
            >
              <X size={18} />
            </button>
            <h3>아이콘 선택</h3>
            <div className="icon-picker">
              {iconChoices.map(([key, label]) => {
                const Icon = iconMap[key];
                return (
                  <button
                    type="button"
                    key={key}
                    title={label}
                    aria-label={label}
                    className={draft.iconKey === key ? "selected" : ""}
                    onClick={() => {
                      setDraft({ ...draft, iconKey: key });
                      setIconPickerOpen(false);
                    }}
                  >
                    <Icon />
                  </button>
                );
              })}
            </div>
          </div>
        </div>
      )}
    </section>
  );
}

function Admin({ canManageSuper }) {
  const [accounts, setAccounts] = useState([]),
    [form, setForm] = useState({
      username: "",
      displayName: "",
      password: "",
      role: "USER",
    }),
    [passwordTarget, setPasswordTarget] = useState(null),
    [newPassword, setNewPassword] = useState(""),
    [message, setMessage] = useState("");
  const load = () =>
    fetch(`${API}/admin/accounts`)
      .then((r) => (r.ok ? r.json() : Promise.reject()))
      .then(setAccounts)
      .catch(() => setMessage("계정 목록을 불러오지 못했습니다."));
  useEffect(() => {
    load();
  }, []);
  const create = async (e) => {
    e.preventDefault();
    setMessage("");
    const r = await fetch(`${API}/admin/accounts`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(form),
    });
    if (!r.ok) {
      setMessage(
        r.status === 409
          ? "이미 존재하는 아이디입니다."
          : "계정을 만들지 못했습니다.",
      );
      return;
    }
    const saved = await r.json();
    setAccounts((v) =>
      [...v, saved].sort((a, b) => a.username.localeCompare(b.username)),
    );
    setForm({ username: "", displayName: "", password: "", role: "USER" });
    setMessage("새 계정을 만들었습니다.");
  };
  const changePassword = async (e) => {
    e.preventDefault();
    const r = await fetch(
      `${API}/admin/accounts/${encodeURIComponent(passwordTarget.username)}/password`,
      {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ password: newPassword }),
      },
    );
    if (!r.ok) {
      setMessage("비밀번호를 변경하지 못했습니다.");
      return;
    }
    setPasswordTarget(null);
    setNewPassword("");
    setMessage("비밀번호를 암호화하여 저장했습니다.");
  };
  return (
    <div className="admin-page">
      <div className="page-title">
        <div>
          <h2>관리</h2>
        </div>
      </div>
      <div className="admin-grid">
        <section className="panel">
          <h3>새 계정 만들기</h3>
          <form className="account-form" onSubmit={create}>
            <label>
              아이디
              <input
                value={form.username}
                onChange={(e) => setForm({ ...form, username: e.target.value })}
                required
              />
            </label>
            <label>
              이름
              <input
                value={form.displayName}
                onChange={(e) =>
                  setForm({ ...form, displayName: e.target.value })
                }
              />
            </label>
            <label>
              비밀번호
              <input
                type="password"
                value={form.password}
                onChange={(e) => setForm({ ...form, password: e.target.value })}
                required
              />
            </label>
            <label>
              권한
              <select
                value={form.role}
                onChange={(e) => setForm({ ...form, role: e.target.value })}
              >
                <option value="USER">사용자</option>
                <option value="ADMIN">관리자</option>
              </select>
            </label>
            <button>계정 생성</button>
          </form>
          {message && <p className="message">{message}</p>}
        </section>
        <section className="panel account-list">
          <h3>계정 목록</h3>
          {accounts.map((account) => (
            <div className="account-row" key={account.username}>
              <div>
                <strong>{account.displayName}</strong>
                <span>
                  {account.username} ·{" "}
                  {account.role === "ADMIN" ? "관리자" : "사용자"}
                </span>
              </div>
              <button disabled={account.role === "SUPER_ADMIN" && !canManageSuper}
                onClick={() => setPasswordTarget(account)}>
                비밀번호 변경
              </button>
            </div>
          ))}
        </section>
      </div>
      {passwordTarget && (
        <div
          className="modal-backdrop"
          onMouseDown={(e) =>
            e.target === e.currentTarget && setPasswordTarget(null)
          }
        >
          <form className="modal" onSubmit={changePassword}>
            <button
              type="button"
              className="close"
              onClick={() => setPasswordTarget(null)}
            >
              <X size={18} />
            </button>
            <h3>비밀번호 변경</h3>
            <p>
              {passwordTarget.displayName} ({passwordTarget.username})
            </p>
            <input
              autoFocus
              type="password"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              placeholder="새 비밀번호"
              required
            />
            <button className="primary">변경</button>
          </form>
        </div>
      )}
    </div>
  );
}

function AccountSettings() {
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [insaUsername, setInsaUsername] = useState("");
  const [insaPassword, setInsaPassword] = useState("");
  const [insaEnabled, setInsaEnabled] = useState(false);
  const [passwordMessage, setPasswordMessage] = useState("");
  const [insaMessage, setInsaMessage] = useState("");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    fetch(`${API}/account/insa`)
      .then((response) => response.ok ? response.json() : Promise.reject())
      .then((value) => { setInsaEnabled(value.enabled); setInsaUsername(value.username || ""); })
      .catch(() => setInsaMessage("자동 로그인 설정을 불러오지 못했습니다."));
  }, []);

  const changePassword = async (event) => {
    event.preventDefault();
    if (newPassword !== confirmPassword) {
      setPasswordMessage("새 비밀번호가 서로 다릅니다.");
      return;
    }
    setBusy(true);
    setPasswordMessage("");
    try {
      const response = await fetch(`${API}/account/password`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ newPassword }),
      });
      if (!response.ok) throw new Error(response.status === 400
        ? "새 비밀번호를 8자 이상 입력해 주세요."
        : "비밀번호를 변경하지 못했습니다.");
      setNewPassword("");
      setConfirmPassword("");
      setPasswordMessage("비밀번호를 변경했습니다.");
    } catch (error) {
      setPasswordMessage(error.message);
    } finally {
      setBusy(false);
    }
  };

  const saveInsa = async (event) => {
    event.preventDefault();
    setBusy(true);
    setInsaMessage("");
    try {
      const response = await fetch(`${API}/account/insa`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username: insaUsername, password: insaPassword }),
      });
      if (!response.ok) throw new Error("자동 로그인 정보를 저장하지 못했습니다.");
      const value = await response.json();
      setInsaEnabled(value.enabled);
      setInsaUsername(value.username);
      setInsaPassword("");
      setInsaMessage("자동 로그인을 설정했습니다. 다음에 인사 관리를 열 때 적용됩니다.");
    } catch (error) {
      setInsaMessage(error.message);
    } finally {
      setBusy(false);
    }
  };

  const removeInsa = async () => {
    setBusy(true);
    setInsaMessage("");
    try {
      const response = await fetch(`${API}/account/insa`, { method: "DELETE" });
      if (!response.ok) throw new Error("자동 로그인을 해제하지 못했습니다.");
      setInsaEnabled(false);
      setInsaUsername("");
      setInsaPassword("");
      setInsaMessage("자동 로그인을 해제했습니다.");
    } catch (error) {
      setInsaMessage(error.message);
    } finally {
      setBusy(false);
    }
  };

  return <section className="panel preference-panel account-settings-panel">
    <div className="preference-heading"><h3>계정 설정</h3></div>
    <form className="account-form" onSubmit={changePassword}>
      <strong>비밀번호 변경</strong>
      <div className="account-settings-fields">
        <label>새 비밀번호<input type="password" autoComplete="new-password" value={newPassword} onChange={(e) => setNewPassword(e.target.value)} minLength={8} required /></label>
        <label>새 비밀번호 확인<input type="password" autoComplete="new-password" value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} minLength={8} required /></label>
      </div>
      <button disabled={busy}>비밀번호 변경</button>
      {passwordMessage && <p className="message" role="status">{passwordMessage}</p>}
    </form>
    <form className="account-form insa-auto-login-form" onSubmit={saveInsa}>
      <strong>인사관리 자동 로그인</strong>
      <div className="account-settings-fields">
        <label>인사관리 계정<input autoComplete="off" value={insaUsername} onChange={(e) => setInsaUsername(e.target.value)} required /></label>
        <label>인사관리 비밀번호<input type="password" autoComplete="new-password" value={insaPassword} onChange={(e) => setInsaPassword(e.target.value)} required /></label>
      </div>
      <div className="account-settings-actions">
        {insaEnabled && <button type="button" className="secondary" disabled={busy} onClick={removeInsa}>자동 로그인 해제</button>}
        <button className="primary" disabled={busy}>자동 로그인 {insaEnabled ? "정보 변경" : "설정"}</button>
      </div>
      {insaMessage && <p className="message" role="status">{insaMessage}</p>}
    </form>
  </section>;
}

function Preferences({
  theme,
  setTheme,
  iconSize,
  setIconSize,
  iconGap,
  setIconGap,
  navCollapsedDefault,
  setNavCollapsedDefault,
  iconColors,
  setIconColor,
  hiddenSiteCodes,
  toggleSiteVisibility,
  isAdmin,
  order,
  setOrder,
  navigationSites,
  onNavigationSitesChange,
  groups,
  onGroupsChange,
  showExternalGroup,
  onShowExternalGroupChange,
}) {
  const [designatedDevice, setDesignatedDevice] = useState(null);
  const [selectedDevices, setSelectedDevices] = useState([]);
  const [deviceBusy, setDeviceBusy] = useState(false);
  const [deviceMessage, setDeviceMessage] = useState("");
  useEffect(() => {
    fetch(`${API}/auth/designated-device`)
      .then((response) => response.ok ? response.json() : Promise.reject())
      .then(setDesignatedDevice)
      .catch(() => setDeviceMessage("지정 PC 정보를 불러오지 못했습니다."));
  }, []);
  const toggleDesignatedDevice = async () => {
    setDeviceBusy(true);
    setDeviceMessage("");
    try {
      const response = await fetch(`${API}/auth/designated-device`, {
        method: designatedDevice?.thisDevice ? "DELETE" : "POST",
      });
      if (!response.ok) throw new Error("지정 PC 설정을 변경하지 못했습니다.");
      if (designatedDevice?.thisDevice) {
        const remaining = (designatedDevice.devices || []).filter((device) => !device.thisDevice);
        setDesignatedDevice({ enabled: remaining.length > 0, thisDevice: false, devices: remaining });
        setSelectedDevices([]);
        setDeviceMessage("지정 PC를 해제했습니다. 현재 로그인은 10분 후 만료됩니다.");
      } else {
        setDesignatedDevice(await response.json());
        setDeviceMessage("이 브라우저를 지정 PC로 추가했습니다. 기존 지정 PC는 유지됩니다.");
      }
    } catch (error) {
      setDeviceMessage(error.message);
    } finally {
      setDeviceBusy(false);
    }
  };
  const removeSelectedDevices = async () => {
    if (!selectedDevices.length) return;
    const removedCurrent = Boolean(designatedDevice?.devices?.some((device) =>
      device.thisDevice && selectedDevices.includes(device.id)));
    setDeviceBusy(true);
    setDeviceMessage("");
    try {
      const response = await fetch(`${API}/auth/designated-device/remove`, {
        method: "POST", headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ ids: selectedDevices }),
      });
      if (!response.ok) throw new Error("선택한 지정 PC를 해제하지 못했습니다.");
      const status = await response.json();
      setDesignatedDevice(status);
      setDeviceMessage(removedCurrent
        ? "선택한 지정 PC를 해제했습니다. 현재 브라우저는 10분 후 다시 로그인해야 합니다."
        : `${selectedDevices.length}개 지정 PC를 해제했습니다.`);
      setSelectedDevices([]);
    } catch (error) {
      setDeviceMessage(error.message);
    } finally {
      setDeviceBusy(false);
    }
  };
  const move = (siteCode, neighborCode) => {
    const fullOrder = [
      ...order.filter((key) => navigationSites.some((site) => site.siteCode === key)),
      ...navigationSites.map((site) => site.siteCode).filter((key) => !order.includes(key)),
    ];
    const index = fullOrder.indexOf(siteCode);
    const target = fullOrder.indexOf(neighborCode);
    if (index < 0 || target < 0) return;
    const next = [...fullOrder];
    [next[index], next[target]] = [next[target], next[index]];
    setOrder(next);
  };
  const designatedDeviceCount = designatedDevice?.devices?.length ?? 0;
  return (
    <div className="settings-page">
      <div className="settings-grid">
        <section className="panel preference-panel navigation-appearance-panel">
          <div className="preference-heading">
            <div>
              <h3>네비게이션 설정</h3>
            </div>
          </div>
          <div className="navigation-settings-columns">
          <div className="navigation-choice-row">
            <div className="compact-setting-row">
              <strong>화면 테마</strong>
              <div className="compact-radio-group" role="radiogroup" aria-label="화면 테마">
                <label><input type="radio" name="screen-theme" value="light" checked={theme === "light"} onChange={() => setTheme("light")} /><span>라이트 모드</span></label>
                <label><input type="radio" name="screen-theme" value="dark" checked={theme === "dark"} onChange={() => setTheme("dark")} /><span>다크 모드</span></label>
              </div>
            </div>
            <div className="compact-setting-row">
              <strong>네비게이션 초기값</strong>
              <div className="compact-radio-group" role="radiogroup" aria-label="네비게이션 초기값">
                <label><input type="radio" name="nav-default" value="expanded" checked={!navCollapsedDefault} onChange={() => setNavCollapsedDefault(false)} /><span>펼침</span></label>
                <label><input type="radio" name="nav-default" value="collapsed" checked={navCollapsedDefault} onChange={() => setNavCollapsedDefault(true)} /><span>접힘</span></label>
              </div>
            </div>
          </div>
          <div className="range-settings">
            <label>
              <strong>아이콘 크기</strong>
              <input
                type="range"
                min="8"
                max="30"
                step="1"
                value={iconSize}
                onChange={(e) => setIconSize(Number(e.target.value))}
              />
              <output>{iconSize}px</output>
            </label>
            <label>
              <strong>아이콘 간격</strong>
              <input
                type="range"
                min="0"
                max="20"
                step="1"
                value={iconGap}
                onChange={(e) => setIconGap(Number(e.target.value))}
              />
              <output>{iconGap}px</output>
            </label>
          </div>
          </div>
          <div className="designated-device-setting">
            <div className="compact-setting-row">
              <strong>지정 PC</strong>
              <div className="designated-device-controls">
                <div className="compact-radio-group" role="radiogroup" aria-label="지정 PC">
                  <label>
                    <input type="radio" name="designated-device" value="regular"
                      checked={designatedDevice !== null && !designatedDevice.thisDevice}
                      disabled={deviceBusy || designatedDevice === null}
                      onChange={() => { if (designatedDevice?.thisDevice) toggleDesignatedDevice(); }} />
                    <span>미지정</span>
                  </label>
                  <label>
                    <input type="radio" name="designated-device" value="designated"
                      checked={Boolean(designatedDevice?.thisDevice)}
                      disabled={deviceBusy || designatedDevice === null}
                      onChange={() => { if (!designatedDevice?.thisDevice) toggleDesignatedDevice(); }} />
                    <span>이 PC 지정</span>
                  </label>
                </div>
                <div className="designated-device-details">
                  {!designatedDevice?.thisDevice && <span>{designatedDevice?.enabled
                      ? "이 PC를 지정해도 기존 지정 PC는 유지됩니다."
                      : "지정하지 않은 PC는 로그인 10분 후 다시 로그인해야 합니다."}</span>}
                  <span className="designated-device-summary">{designatedDeviceCount ? `지정 ${designatedDeviceCount}대` : "지정된 PC 없음"}</span>
                </div>
              </div>
            </div>
            {designatedDeviceCount > 0 && <div className="designated-device-list" aria-label="지정 PC 목록">
              <div className="designated-device-actions">
                <button type="button" disabled={deviceBusy || !selectedDevices.length} onClick={removeSelectedDevices}>
                  지정PC 해제
                </button>
              </div>
              {(designatedDevice.devices || []).map((device) => <label className="designated-device-item" key={device.id}>
                <input type="checkbox" checked={selectedDevices.includes(device.id)} disabled={deviceBusy}
                  onChange={() => setSelectedDevices((current) => current.includes(device.id)
                    ? current.filter((id) => id !== device.id) : [...current, device.id])} />
                <span>등록 IP {device.ipAddress || "확인 불가"} · 기기 #{device.id.slice(0, 8)}</span>
              </label>)}
            </div>}
          </div>
          {deviceMessage && <p className="message" role="status">{deviceMessage}</p>}
        </section>
        <AccountSettings />
        <SiteManagement
          sites={navigationSites}
          order={order}
          onMove={move}
          onChange={onNavigationSitesChange}
          groups={groups}
          onGroupsChange={onGroupsChange}
          showExternalGroup={showExternalGroup}
          onShowExternalGroupChange={onShowExternalGroupChange}
          iconColors={iconColors}
          onIconColorChange={setIconColor}
          hiddenSiteCodes={hiddenSiteCodes}
          onVisibilityChange={toggleSiteVisibility}
          isAdmin={isAdmin}
        />
      </div>
    </div>
  );
}

const workspaceKey = (username) => `srgh-it-workspace:${username}`;
const readWorkspace = (username) => {
  try { return JSON.parse(sessionStorage.getItem(workspaceKey(username)) || "null"); }
  catch { return null; }
};
const saveWorkspace = (username, state) => {
  try { sessionStorage.setItem(workspaceKey(username), JSON.stringify(state)); }
  catch { /* Session storage may be unavailable in a restricted browser. */ }
};
const portalDeepLinkBases = [
  "/work", "/worklog", "/management", "/note", "/register", "/server",
  "/account", "/vendor", "/install", "/rental",
];
const requestedPortalPath = () => {
  if (!window.location.hash.startsWith("#next=")) return null;
  const raw = window.location.hash.slice(6);
  if (!raw.startsWith("/") || raw.startsWith("//") || raw.length > 4096 || /[\\\r\n]/.test(raw)) return null;
  try {
    const page = new URL(raw, window.location.origin);
    if (page.origin !== window.location.origin || !portalDeepLinkBases.some((base) =>
      page.pathname === base || page.pathname.startsWith(`${base}/`))) return null;
    if (page.pathname === "/work" || page.pathname.startsWith("/work/")) return "/worklog/";
    return `${page.pathname}${page.search}${page.hash}`;
  } catch { return null; }
};
const restorablePath = (path, siteUrl) => {
  if (!path) return false;
  try {
    const page = new URL(path, window.location.origin);
    const site = new URL(siteUrl, window.location.origin);
    const base = site.pathname.replace(/\/$/, "");
    const isWorkSite = base === "/worklog";
    const isWorkPage = /^\/worklog(?:\/|$)/.test(page.pathname);
    return page.origin === window.location.origin && site.origin === page.origin &&
      ((isWorkSite && isWorkPage) || base === "" || page.pathname === base || page.pathname.startsWith(`${base}/`));
  } catch { return false; }
};

// A versioned iframe URL prevents an already-cached worklog shell from loading
// the old fixed-width legacy table after the sidebar is expanded.
const embeddedSitePath = (path, siteUrl) => {
  const resolved = restorablePath(path, siteUrl) ? path : siteUrl;
  const url = new URL(resolved, window.location.origin);
  if (url.origin === window.location.origin && portalDeepLinkBases.some((base) =>
    url.pathname === base || url.pathname.startsWith(`${base}/`))) {
    // Exact app roots redirect to a trailing slash in nginx; carry the embed
    // marker to the final URL so /it is never loaded inside its own iframe.
    if (portalDeepLinkBases.includes(url.pathname)) url.pathname += "/";
    url.searchParams.set("it_embed", "1");
    if (/^\/worklog(?:\/|$)/.test(url.pathname)) url.searchParams.set("embedLayout", "20261011c");
    // /management now serves the Java app. Give its iframe a new URL so a
    // browser cannot reuse the retired Python app's cached document.
    if (/^\/management(?:\/|$)/.test(url.pathname)) url.searchParams.set("managementUi", "java-20261011");
    return `${url.pathname}${url.search}${url.hash}`;
  }
  if (url.origin !== window.location.origin ||
      !/^\/worklog(?:\/|$)/.test(url.pathname)) return resolved;
  // /worklog redirects to /worklog/ and drops its query string in nginx.
  if (url.pathname === "/worklog") url.pathname = "/worklog/";
  url.searchParams.set("embedLayout", "20261011c");
  return `${url.pathname}${url.search}${url.hash}`;
};

function Shell({ user, onLogout }) {
  const [savedWorkspace] = useState(() => readWorkspace(user.username));
  const [collapsed, setCollapsed] = useState(false),
    [navCollapsedDefault, setNavCollapsedDefaultState] = useState(false),
    [settingsReady, setSettingsReady] = useState(false),
    [externalNavOpen, setExternalNavOpen] = useState(false),
    [theme, setThemeState] = useState("light"),
    [iconSize, setIconSizeState] = useState(18),
    [iconGap, setIconGapState] = useState(5),
    [iconColors, setIconColorsState] = useState(defaultIconColors),
    [hiddenSiteCodes, setHiddenSiteCodes] = useState([]),
    [navigationSites, setNavigationSites] = useState(fallbackSites),
    [groups, setGroupsState] = useState([]),
    [showExternalGroup, setShowExternalGroupState] = useState(true),
    [order, setOrderState] = useState(defaultOrder),
    [currentKey, setCurrentKey] = useState(savedWorkspace?.key || "work"),
    [view, setView] = useState(savedWorkspace?.view === "settings" ||
      (savedWorkspace?.view === "admin" && ["ADMIN", "SUPER_ADMIN"].includes(user.role)) ? savedWorkspace.view : "site"),
    [iframeSrc, setIframeSrc] = useState(savedWorkspace?.path || null),
    [managementVisit, setManagementVisit] = useState(0);
  const appearanceSaveTimer = useRef(null);
  const iframeRef = useRef(null);
  const iframeLocation = useRef({ key: savedWorkspace?.key, path: savedWorkspace?.path });
  const catalog = navigationSites.map((site) => ({
    ...site,
    key: site.siteCode,
    url: site.siteCode === "work" ? "/worklog" : site.url,
    icon: iconMap[site.iconKey] || Link2,
  }));
  const sites = order
    .map((key) => catalog.find((site) => site.key === key))
    .filter(Boolean);
  const visibleSites = sites.filter((site) => !hiddenSiteCodes.includes(site.key));
  const current = catalog.find((site) => site.key === currentKey) ||
    visibleSites[0] || { key: "empty", name: "사이트 없음", url: "about:blank" };
  const groupedCodes = new Set(groups.flatMap((group) => group.siteCodes));
  const internalSites = visibleSites.filter((site) => !groupedCodes.has(site.key)
    && (!site.external || !showExternalGroup));
  const externalSites = visibleSites.filter((site) => site.external && !groupedCodes.has(site.key));
  const captureIframeLocation = () => {
    if (view !== "site" || !iframeRef.current) return;
    try {
      const path = iframeRef.current.contentWindow.location;
      const savedPath = `${path.pathname}${path.search}${path.hash}`;
      if (!restorablePath(savedPath, current.url)) return;
      iframeLocation.current = { key: current.key, path: savedPath };
      saveWorkspace(user.username, { view, key: current.key, path: savedPath });
    } catch { /* External sites cannot be inspected. */ }
  };
  useEffect(() => {
    const onIframeLocation = (event) => {
      if (event.origin !== window.location.origin ||
          event.source !== iframeRef.current?.contentWindow ||
          event.data?.type !== "srgh-it:iframe-location") return;
      captureIframeLocation();
    };
    window.addEventListener("message", onIframeLocation);
    return () => window.removeEventListener("message", onIframeLocation);
  }, [view, current.key, current.url, user.username]);
  useEffect(() => {
    if (!settingsReady) return;
    saveWorkspace(user.username, {
      view, key: current.key,
      path: iframeLocation.current.key === current.key ? iframeLocation.current.path : null,
    });
  }, [settingsReady, view, current.key, user.username]);
  useEffect(() => {
    if (!settingsReady || view !== "site") return;
    const timer = window.setInterval(captureIframeLocation, 1000);
    return () => window.clearInterval(timer);
  }, [settingsReady, view, current.key, current.url]);
  const openView = (nextView) => {
    captureIframeLocation();
    if (iframeLocation.current.key === current.key) setIframeSrc(iframeLocation.current.path);
    setView(nextView);
  };
  useEffect(() => {
    document.documentElement.dataset.theme = theme;
  }, [theme]);
  useEffect(() => {
    fetch(`${API}/settings`)
      .then((response) => (response.ok ? response.json() : Promise.reject()))
      .then((settings) => {
        setThemeState(settings.theme || "light");
        setNavigationSites(settings.sites || fallbackSites);
        setOrderState(settings.order || defaultOrder);
        setIconSizeState(settings.iconSize ?? 18);
        setIconGapState(settings.iconGap ?? 5);
        setNavCollapsedDefaultState(settings.navCollapsedDefault ?? false);
        setCollapsed(settings.navCollapsedDefault ?? false);
        setIconColorsState({ ...defaultIconColors, ...(settings.iconColors || {}) });
        setHiddenSiteCodes(settings.hiddenSiteCodes || []);
        setGroupsState(settings.groups || []);
        setShowExternalGroupState(settings.showExternalGroup !== false);
      })
      .catch(() => {
        setNavigationSites(fallbackSites);
        setOrderState(defaultOrder);
      })
      .finally(() => setSettingsReady(true));
  }, []);
  useEffect(() => {
    if (!settingsReady) return;
    const openDeepLink = () => {
      const requestedPath = requestedPortalPath();
      if (!requestedPath) return;
      const site = catalog.filter((entry) => !entry.external && restorablePath(requestedPath, entry.url))
        .sort((left, right) => right.url.length - left.url.length)[0];
      if (!site) return;
      iframeLocation.current = { key: site.key, path: requestedPath };
      setCurrentKey(site.key);
      setIframeSrc(requestedPath);
      setView("site");
      window.history.replaceState(null, "", `${window.location.pathname}${window.location.search}`);
    };
    openDeepLink();
    window.addEventListener("hashchange", openDeepLink);
    return () => window.removeEventListener("hashchange", openDeepLink);
  }, [settingsReady, navigationSites]);
  const saveSettings = (changes) =>
    fetch(`${API}/settings`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        theme,
        sites: navigationSites,
        order,
        iconSize,
        iconGap,
        navCollapsedDefault,
        iconColors,
        hiddenSiteCodes,
        groups,
        showExternalGroup,
        ...changes,
      }),
    });
  const setTheme = (value) => {
    setThemeState(value);
    saveSettings({ theme: value });
  };
  const saveAppearance = (changes) => {
    clearTimeout(appearanceSaveTimer.current);
    appearanceSaveTimer.current = setTimeout(() => saveSettings(changes), 250);
  };
  const setIconSize = (value) => {
    setIconSizeState(value);
    saveAppearance({ iconSize: value, iconGap });
  };
  const setIconGap = (value) => {
    setIconGapState(value);
    saveAppearance({ iconSize, iconGap: value });
  };
  const setNavCollapsedDefault = (value) => {
    clearTimeout(appearanceSaveTimer.current);
    setNavCollapsedDefaultState(value);
    setCollapsed(value);
    saveSettings({ navCollapsedDefault: value, iconSize, iconGap, iconColors });
  };
  const setIconColor = (key, value) => {
    const next = { ...iconColors, [key]: value };
    setIconColorsState(next);
    saveAppearance({ iconSize, iconGap, iconColors: next });
  };
  const setOrder = (value) => {
    setOrderState(value);
    saveSettings({ order: value });
  };
  const setSites = (value, colorUpdates = {}) => {
    const available = value.map((site) => site.siteCode);
    const nextOrder = [
      ...order.filter((key) => available.includes(key)),
      ...available.filter((key) => !order.includes(key)),
    ];
    setNavigationSites(value);
    setOrderState(nextOrder);
    const nextColors = { ...defaultIconColors, ...iconColors, ...colorUpdates };
    value.forEach((site) => { if (!nextColors[site.siteCode]) nextColors[site.siteCode] = "#2563eb"; });
    setIconColorsState(nextColors);
    const nextGroups = groups.map((group) => ({ ...group,
      siteCodes: group.siteCodes.filter((code) => available.includes(code)) }));
    setGroupsState(nextGroups);
    saveSettings({ sites: value, order: nextOrder, iconColors: nextColors, groups: nextGroups });
  };
  const setGroups = (value) => {
    setGroupsState(value);
    saveSettings({ groups: value });
  };
  const setShowExternalGroup = (value) => {
    setShowExternalGroupState(value);
    saveSettings({ showExternalGroup: value });
  };
  const toggleSiteVisibility = (siteCode) => {
    const next = hiddenSiteCodes.includes(siteCode)
      ? hiddenSiteCodes.filter((code) => code !== siteCode)
      : [...hiddenSiteCodes, siteCode];
    setHiddenSiteCodes(next);
    saveSettings({ hiddenSiteCodes: next });
  };
  const select = async (site) => {
    // The extension directory is public and must outlive the /it session on non-designated PCs.
    if (site.external || site.key === "extension") {
      window.open(site.url, "_blank", "noopener,noreferrer");
      return;
    }
    captureIframeLocation();
    if (site.url.replace(/\/$/, "") === "/insa") {
      try {
        const settingsResponse = await fetch(`${API}/account/insa`);
        if (!settingsResponse.ok) throw new Error("인사관리 자동 로그인 설정을 확인하지 못했습니다.");
        const settings = await settingsResponse.json();
        if (settings.enabled) {
          let alreadySignedIn = false;
          const existingToken = localStorage.getItem("accessToken");
          if (existingToken) {
            const sessionResponse = await fetch("/insa-api/users/me", {
              headers: { Authorization: `Bearer ${existingToken}` },
            });
            if (sessionResponse.ok) {
              const sessionUser = await sessionResponse.json();
              alreadySignedIn = sessionUser.username === settings.username;
            }
          }
          if (!alreadySignedIn) {
            const startSession = (force) => fetch(`${API}/account/insa/session`, {
              method: "POST",
              headers: { "Content-Type": "application/json" },
              body: JSON.stringify({ force }),
            });
            let loginResponse = await startSession(false);
            if (loginResponse.status === 409) {
              if (!window.confirm("인사관리 계정이 다른 곳에서 사용 중입니다. 기존 세션을 종료하고 로그인할까요?")) return;
              loginResponse = await startSession(true);
            }
            if (!loginResponse.ok) throw new Error(loginResponse.status === 401
              ? "인사관리 계정 또는 비밀번호를 확인해 주세요. 설정에서 다시 등록할 수 있습니다."
              : "인사관리 자동 로그인에 실패했습니다. 잠시 후 다시 시도해 주세요.");
            const session = await loginResponse.json();
            localStorage.setItem("accessToken", session.accessToken);
          }
        }
      } catch (error) {
        window.alert(error.message || "인사관리 자동 로그인에 실패했습니다.");
        return;
      }
    }
    const rememberedPath = iframeLocation.current.key === site.key &&
      restorablePath(iframeLocation.current.path, site.url) ? iframeLocation.current.path : site.url;
    iframeLocation.current = { key: site.key, path: rememberedPath };
    setIframeSrc(rememberedPath);
    if (site.key === "management") setManagementVisit((visit) => visit + 1);
    setCurrentKey(site.key);
    setView("site");
  };
  if (!settingsReady) return <div className="loading">SRGH IT</div>;
  return (
    <div
      className={`shell ${collapsed ? "collapsed" : ""}`}
      style={{
        "--nav-icon-size": `${iconSize}px`,
        "--nav-icon-gap": `${iconGap}px`,
      }}
    >
      <aside>
        <div className="side-head">
          <div className="logo">
            <span>SRGH IT</span>
          </div>
          <button
            title={collapsed ? "메뉴 펼치기" : "메뉴 접기"}
            onClick={() => setCollapsed((v) => !v)}
          >
            {collapsed ? <ChevronRight /> : <ChevronLeft />}
          </button>
        </div>
        <nav>
          {internalSites.map((site) => {
            const Icon = site.icon;
            return (
              <button
                title={site.name}
                className={
                  view === "site" && current.key === site.key ? "active" : ""
                }
                key={site.key}
                onClick={() => select(site)}
              >
                <i style={{ color: iconColors[site.key] || "#2563eb" }}>
                  <Icon />
                </i>
                <span>{site.name}</span>
              </button>
            );
          })}
          {groups.map((group) => {
            const members = visibleSites.filter((site) => group.siteCodes.includes(site.key));
            return <div className="external-nav-group" key={group.id}>
              <button type="button" title={group.name} className="external-nav-toggle"
                aria-expanded={!group.collapsed} onClick={() => setGroups(groups.map((item) =>
                  item.id === group.id ? { ...item, collapsed: !item.collapsed } : item))}>
                <i style={{ color: iconColors[group.id] || "#2563eb" }}><Folder /></i>
                <span>{group.name}</span>
                <ChevronDown className={group.collapsed ? "" : "open"} />
              </button>
              {!group.collapsed && <div className="external-nav-items">{members.map((site) => {
                const Icon = site.icon;
                return <button key={site.key} title={site.name}
                  className={view === "site" && current.key === site.key ? "active" : ""}
                  onClick={() => select(site)}>
                  <i style={{ color: iconColors[site.key] || "#2563eb" }}><Icon /></i>
                  <span>{site.name}</span>
                </button>;
              })}</div>}
            </div>;
          })}
          {showExternalGroup && externalSites.length > 0 && (
            <div className="external-nav-group">
              <button
                type="button"
                title="외부사이트"
                className="external-nav-toggle"
                onClick={() => setExternalNavOpen((open) => !open)}
              >
                <i style={{ color: iconColors.external || "#2563eb" }}>
                  <Globe2 />
                </i>
                <span>외부사이트</span>
                <ChevronDown className={externalNavOpen ? "open" : ""} />
              </button>
              {externalNavOpen && (
                <div className="external-nav-items">
                  {externalSites.map((site) => {
                    const Icon = site.icon;
                    return (
                      <button
                        title={site.name}
                        key={site.key}
                        onClick={() => select(site)}
                      >
                        <i style={{ color: iconColors[site.key] || "#2563eb" }}>
                          <Icon />
                        </i>
                        <span>{site.name}</span>
                      </button>
                    );
                  })}
                </div>
              )}
            </div>
          )}
        </nav>
        <div className="side-bottom">
          <button
            title="설정"
            className={view === "settings" ? "active" : ""}
            onClick={() => openView("settings")}
          >
            <i style={{ color: iconColors.settings || "#2563eb" }}><Settings /></i>
            <span>설정</span>
          </button>
          {["ADMIN", "SUPER_ADMIN"].includes(user.role) && (
            <button
              title="관리"
              className={view === "admin" ? "active" : ""}
              onClick={() => openView("admin")}
            >
              <i style={{ color: iconColors.admin || "#2563eb" }}><UserCog /></i>
              <span>관리</span>
            </button>
          )}
          <button title="로그아웃" onClick={onLogout}>
            <i style={{ color: iconColors.logout || "#2563eb" }}><LogOut /></i>
            <span>로그아웃</span>
          </button>
        </div>
      </aside>
      <section className="workspace">
        <div
          className="workspace-content"
          key={view === "site" ? `site-${current.url}-${current.key === "management" ? managementVisit : 0}` : view}
        >
          {view === "admin" ? (
            <Admin canManageSuper={user.role === "SUPER_ADMIN"} />
          ) : view === "settings" ? (
            <Preferences
              theme={theme}
              setTheme={setTheme}
              iconSize={iconSize}
              setIconSize={setIconSize}
              iconGap={iconGap}
              setIconGap={setIconGap}
              navCollapsedDefault={navCollapsedDefault}
              setNavCollapsedDefault={setNavCollapsedDefault}
              iconColors={iconColors}
              setIconColor={setIconColor}
              hiddenSiteCodes={hiddenSiteCodes}
              toggleSiteVisibility={toggleSiteVisibility}
              isAdmin={["ADMIN", "SUPER_ADMIN"].includes(user.role)}
              order={order}
              setOrder={setOrder}
              navigationSites={navigationSites}
              onNavigationSitesChange={setSites}
              groups={groups}
              onGroupsChange={setGroups}
              showExternalGroup={showExternalGroup}
              onShowExternalGroupChange={setShowExternalGroup}
            />
          ) : (
            <iframe ref={iframeRef} title={current.name}
              src={embeddedSitePath(iframeSrc, current.url)}
              onLoad={captureIframeLocation} />
          )}
        </div>
      </section>
    </div>
  );
}

function App() {
  const [user, setUser] = useState(undefined);
  useEffect(() => {
    document.documentElement.dataset.theme = "light";
    fetch(`${API}/auth/me`)
      .then((r) => (r.ok ? r.json() : null))
      .then(setUser)
      .catch(() => setUser(null));
  }, []);
  useEffect(() => {
    if (!user) return undefined;
    const renew = () => {
      fetch(`${API}/auth/me`, { cache: "no-store", credentials: "same-origin" })
        .then((response) => { if (response.status === 401) setUser(null); })
        .catch(() => { /* A temporary network error must not log out an active user. */ });
    };
    const timer = window.setInterval(renew, 15 * 1000);
    return () => window.clearInterval(timer);
  }, [user?.username]);
  const logout = async () => {
    await fetch(`${API}/auth/logout`, { method: "POST" });
    if (user?.username) sessionStorage.removeItem(workspaceKey(user.username));
    setUser(null);
  };
  if (user === undefined) return <div className="loading">SRGH IT</div>;
  return user ? (
    <Shell user={user} onLogout={logout} />
  ) : (
    <Login onLogin={setUser} />
  );
}

createRoot(document.getElementById("root")).render(<App />);
