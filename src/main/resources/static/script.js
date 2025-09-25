const API_URL = "http://localhost:8080/api/tasks";

async function loadTasks() {
    const res = await fetch(API_URL);
    const tasks = await res.json();
    const list = document.getElementById("taskList");
    list.innerHTML = "";
    tasks.forEach(task => {
        const li = document.createElement("li");
        li.innerHTML = `
            <input type="checkbox" ${task.completed ? "checked" : ""} onclick="toggleTask(${task.id}, ${!task.completed})">
            ${task.title}
            <button onclick="deleteTask(${task.id})">Delete</button>
        `;
        list.appendChild(li);
    });
}

async function addTask() {
    const title = document.getElementById("taskInput").value;
    if (!title) return;

    await fetch(API_URL, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ title, completed: false })
    });

    document.getElementById("taskInput").value = "";
    loadTasks();
}

async function toggleTask(id, completed) {
    await fetch(`${API_URL}/${id}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ title: "", completed })
    });
    loadTasks();
}

async function deleteTask(id) {
    await fetch(`${API_URL}/${id}`, {
        method: "DELETE"
    });
    loadTasks();
}

loadTasks();
